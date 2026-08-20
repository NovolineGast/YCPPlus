#include "native_jvm.hpp"
#include <algorithm>
#include <stdio.h>
#include <stddef.h>
#include <string>
#include <vector>
#include <cstdlib>
#include <cctype>

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include "VMProtectSDK.h"
#else
// POSIX 构建仅用于开发与测试授权客户端（正式产物始终面向 Windows）
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <unistd.h>
#endif

namespace native_jvm::utils {

    // ===== 开放授权客户端（替代闭源 Authorization.dll）=====
    // 协议与自建授权服务器对齐：POST <serverURL> 表单 app=<应用名>&key=<密钥>，响应体 "success" 即通过
    // 密钥来源优先级：环境变量 YCP_LICENSE_KEY → 工作目录 license.key 文件 → 交互式输入（Windows 为密钥对话框）

#ifdef _WIN32
#define YCP_STR(s) VMProtectDecryptStringA(s)
#define YCP_PROTECT_BEGIN() VMProtectBeginUltra("authorization")
#define YCP_PROTECT_END() VMProtectEnd()
#else
#define YCP_STR(s) (s)
#define YCP_PROTECT_BEGIN() ((void) 0)
#define YCP_PROTECT_END() ((void) 0)
#endif

    struct ParsedURL {
        std::string host;
        int port = 80;
        std::string path = "/";
        bool valid = false;
    };

    // 解析 http://host[:port]/path 形式的授权地址
    static ParsedURL parse_url(const std::string &url) {
        ParsedURL out;
        std::string rest;
        if (url.rfind("http://", 0) == 0) rest = url.substr(7);
        else if (url.rfind("https://", 0) == 0) return out; // 与原实现一致：仅明文 HTTP
        else rest = url;

        size_t slash = rest.find('/');
        if (slash == std::string::npos) out.host = rest;
        else {
            out.host = rest.substr(0, slash);
            out.path = rest.substr(slash);
        }
        size_t colon = out.host.rfind(':');
        if (colon != std::string::npos) {
            out.port = atoi(out.host.substr(colon + 1).c_str());
            out.host = out.host.substr(0, colon);
        }
        out.valid = !out.host.empty() && out.port > 0 && out.port < 65536;
        return out;
    }

    static std::string url_encode(const std::string &value) {
        static const char *hex = "0123456789ABCDEF";
        std::string result;
        result.reserve(value.size() * 3);
        for (unsigned char c : value) {
            if (std::isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
                result += (char) c;
            } else {
                result += '%';
                result += hex[c >> 4];
                result += hex[c & 0xF];
            }
        }
        return result;
    }

    static std::string &trim_inplace(std::string &s) {
        size_t b = s.find_first_not_of(" \t\r\n");
        if (b == std::string::npos) { s.clear(); return s; }
        size_t e = s.find_last_not_of(" \t\r\n");
        s = s.substr(b, e - b + 1);
        return s;
    }

    // 简易 chunked 解码（gunicorn 等服务器可能启用分块传输）
    static std::string decode_chunked(const std::string &in) {
        std::string out;
        size_t pos = 0;
        while (pos < in.size()) {
            size_t eol = in.find("\r\n", pos);
            if (eol == std::string::npos) break;
            unsigned long size = strtoul(in.substr(pos, eol - pos).c_str(), nullptr, 16);
            if (size == 0) break;
            size_t data_start = eol + 2;
            if (data_start + size > in.size()) break;
            out.append(in, data_start, size);
            pos = data_start + size + 2;
        }
        return out;
    }

    // 跨平台 HTTP POST（Windows: WinSock2 / POSIX: socket，用于开发测试）
    static bool http_post(const ParsedURL &url, const std::string &body, int timeout_seconds, std::string &response) {
#ifdef _WIN32
        WSADATA wsa;
        static bool wsa_ready = false;
        if (!wsa_ready) {
            if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) return false;
            wsa_ready = true;
        }
#endif
        struct addrinfo hints{}, *res = nullptr;
        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_STREAM;
        if (getaddrinfo(url.host.c_str(), std::to_string(url.port).c_str(), &hints, &res) != 0 || !res)
            return false;

        int fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
        if (fd < 0) { freeaddrinfo(res); return false; }

#ifdef _WIN32
        DWORD timeout_ms = timeout_seconds * 1000;
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &timeout_ms, sizeof(timeout_ms));
        setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, (const char *) &timeout_ms, sizeof(timeout_ms));
#else
        struct timeval tv{};
        tv.tv_sec = timeout_seconds;
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
#endif

        bool ok = connect(fd, res->ai_addr, (int) res->ai_addrlen) == 0;
        freeaddrinfo(res);
        if (!ok) {
#ifdef _WIN32
            closesocket(fd);
#else
            close(fd);
#endif
            return false;
        }

        std::string request;
        request += "POST " + url.path + " HTTP/1.1\r\n";
        request += "Host: " + url.host + (url.port != 80 ? ":" + std::to_string(url.port) : "") + "\r\n";
        request += "Content-Type: application/x-www-form-urlencoded\r\n";
        request += "Content-Length: " + std::to_string(body.size()) + "\r\n";
        request += "Connection: close\r\n\r\n";
        request += body;

        ok = send(fd, request.c_str(), (int) request.size(), 0) == (int) request.size();

        std::string raw;
        if (ok) {
            char buf[4096];
            int n;
            while ((n = recv(fd, buf, sizeof(buf), 0)) > 0)
                raw.append(buf, (size_t) n);
        }

#ifdef _WIN32
        closesocket(fd);
#else
        close(fd);
#endif
        if (!ok || raw.empty()) return false;

        size_t header_end = raw.find("\r\n\r\n");
        if (header_end == std::string::npos) return false;
        std::string headers = raw.substr(0, header_end);
        std::string content = raw.substr(header_end + 4);

        std::string lower;
        lower.reserve(headers.size());
        for (char c : headers) lower += (char) std::tolower((unsigned char) c);
        if (lower.find("transfer-encoding: chunked") != std::string::npos)
            content = decode_chunked(content);

        response = content;
        return true;
    }

    // 从工作目录 license.key 读取密钥（支持 BOM 与首尾空白）
    static std::string read_key_from_file() {
        FILE *f = fopen(YCP_STR("license.key"), "rb");
        if (!f) return "";
        std::string key;
        char buf[512];
        size_t n;
        while ((n = fread(buf, 1, sizeof(buf), f)) > 0) key.append(buf, n);
        fclose(f);
        if (key.size() >= 3 && (unsigned char) key[0] == 0xEF && (unsigned char) key[1] == 0xBB && (unsigned char) key[2] == 0xBF)
            key = key.substr(3);
        return trim_inplace(key);
    }

#ifdef _WIN32
    // —— 无资源文件的密钥输入对话框（内存构造 DLGTEMPLATE）——
    struct KeyDialogCtx {
        char key[512];
        char title[160];
    };

    static INT_PTR CALLBACK key_dlg_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
        switch (msg) {
            case WM_INITDIALOG: {
                KeyDialogCtx *ctx = (KeyDialogCtx *) lp;
                SetWindowLongPtrA(hwnd, GWLP_USERDATA, (LONG_PTR) ctx);
                SetWindowTextA(hwnd, ctx->title);
                return TRUE;
            }
            case WM_COMMAND:
                if (LOWORD(wp) == IDOK || LOWORD(wp) == IDCANCEL) {
                    KeyDialogCtx *ctx = (KeyDialogCtx *) GetWindowLongPtrA(hwnd, GWLP_USERDATA);
                    if (LOWORD(wp) == IDOK) GetDlgItemTextA(hwnd, 101, ctx->key, sizeof(ctx->key));
                    EndDialog(hwnd, LOWORD(wp) == IDOK ? 1 : 0);
                    return TRUE;
                }
                break;
            case WM_CLOSE:
                EndDialog(hwnd, 0);
                return TRUE;
        }
        return FALSE;
    }

    static std::vector<BYTE> build_key_dlg_template() {
        std::vector<BYTE> buf;
        auto put = [&buf](const void *data, size_t len) {
            const BYTE *p = (const BYTE *) data;
            buf.insert(buf.end(), p, p + len);
        };
        auto align4 = [&buf]() { while (buf.size() % sizeof(DWORD)) buf.push_back(0); };
        auto put_word = [&buf](WORD w) { buf.push_back((BYTE) w); buf.push_back((BYTE) (w >> 8)); };
        auto put_wstr = [&buf, &align4, &put_word](const wchar_t *s) {
            align4();
            while (*s) { put_word((WORD) *s); s++; }
            put_word(0);
        };

        DLGTEMPLATE dt{};
        dt.style = WS_POPUP | WS_CAPTION | WS_SYSMENU | DS_MODALFRAME | DS_CENTER;
        dt.cdit = 3;
        dt.cx = 240;
        dt.cy = 76;
        put(&dt, sizeof(dt));
        put_word(0);                    // 菜单：无
        put_word(0);                    // 窗口类：预定义对话框类
        put_wstr(L"YumeCloudProtection");

        // 依次注册 Static(提示) / Edit(输入) / Button(确定)，类原子 0x0082/0x0081/0x0080
        auto item = [&](DWORD style, WORD x, WORD y, WORD cx, WORD cy, WORD id, WORD class_atom, const wchar_t *title) {
            align4();
            DLGITEMTEMPLATE it{};
            it.style = style;
            it.x = x; it.y = y; it.cx = cx; it.cy = cy;
            it.id = id;
            put(&it, sizeof(it));
            put_word(class_atom);
            put_wstr(title);
            put_word(0);                // creation data：无
        };

        item(WS_CHILD | WS_VISIBLE | SS_LEFT, 10, 10, 220, 8, (WORD) -1, 0x0082, L"Please enter your license key:");
        item(WS_CHILD | WS_VISIBLE | WS_TABSTOP | WS_BORDER | ES_AUTOHSCROLL, 10, 24, 220, 14, 101, 0x0081, L"");
        item(WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_DEFPUSHBUTTON, 90, 48, 60, 14, IDOK, 0x0080, L"OK");
        return buf;
    }

    static std::string prompt_key_dialog(const char *application_name) {
        KeyDialogCtx ctx{};
        snprintf(ctx.title, sizeof(ctx.title), "%s - License", application_name);
        std::vector<BYTE> tmpl = build_key_dlg_template();
        INT_PTR r = DialogBoxIndirectParamA((HINSTANCE) GetModuleHandleA(nullptr),
                                            (LPCDLGTEMPLATE) tmpl.data(), nullptr, key_dlg_proc, (LPARAM) &ctx);
        if (r == 1 && ctx.key[0]) return std::string(ctx.key);
        return "";
    }
#endif

    static std::string prompt_key(const char *application_name) {
#ifdef _WIN32
        return prompt_key_dialog(application_name);
#else
        fprintf(stderr, "[%s] %s", application_name, YCP_STR("please enter your license key: "));
        char buf[512] = {0};
        if (!fgets(buf, sizeof(buf), stdin)) return "";
        std::string key(buf);
        return trim_inplace(key);
#endif
    }

    static void notify_invalid_key(const char *application_name) {
#ifdef _WIN32
        MessageBoxA(nullptr, YCP_STR("Invalid or expired license key."), application_name, MB_ICONERROR | MB_OK);
#else
        fprintf(stderr, "[%s] %s\n", application_name, YCP_STR("invalid or expired license key"));
#endif
    }

    bool auth(const char *applicationName, const char *serverURL) {
        YCP_PROTECT_BEGIN();

        ParsedURL url = parse_url(serverURL);
        if (!url.valid) {
            fprintf(stderr, "[YumeCloudProtection] %s: %s\n", YCP_STR("invalid authorization server URL"), serverURL);
            YCP_PROTECT_END();
            return false;
        }

        // 非交互来源：环境变量 → license.key 文件
        std::string key;
        const char *env = getenv(YCP_STR("YCP_LICENSE_KEY"));
        if (env && *env) { key = env; trim_inplace(key); }
        if (key.empty()) key = read_key_from_file();

        // 交互模式（弹窗/控制台输入）最多尝试 3 次；非交互密钥只验证 1 次
        bool interactive = key.empty();
        int max_attempts = interactive ? 3 : 1;

        for (int attempt = 0; attempt < max_attempts; attempt++) {
            if (interactive)
                key = prompt_key(applicationName);
            if (key.empty())
                break; // 用户取消

            std::string body = std::string(YCP_STR("app=")) + url_encode(applicationName)
                             + YCP_STR("&key=") + url_encode(key);
            std::string response;
            if (http_post(url, body, 10, response) && trim_inplace(response) == YCP_STR("success")) {
                YCP_PROTECT_END();
                return true;
            }

            if (attempt + 1 < max_attempts)
                notify_invalid_key(applicationName);
            key.clear();
        }

        fprintf(stderr, "[YumeCloudProtection] %s\n", YCP_STR("license validation failed"));
        YCP_PROTECT_END();
        return false;
    }

    jclass boolean_array_class;
    jmethodID string_intern_method;
    jclass class_class;
    jmethodID get_classloader_method;
    jclass object_class;
    jmethodID get_class_method;
    jclass classloader_class;
    jmethodID load_class_method;
    jclass no_class_def_found_class;
    jmethodID ncdf_init_method;
    jclass throwable_class;
    jmethodID get_message_method;
    jmethodID init_cause_method;
    jclass methodhandles_lookup_class;
    jmethodID lookup_init_method;
#ifdef USE_HOTSPOT
    jclass methodhandle_natives_class;
    jmethodID link_call_site_method;
    bool is_jvm11_link_call_site;
#endif

    void init_utils(JNIEnv *env) {
        jclass clazz = env->FindClass("[Z");
        if (env->ExceptionCheck())
            return;
        boolean_array_class = (jclass) env->NewGlobalRef(clazz);
        env->DeleteLocalRef(clazz);

        jclass string_clazz = env->FindClass("java/lang/String");
        if (env->ExceptionCheck())
            return;
        string_intern_method = env->GetMethodID(string_clazz, "intern", "()Ljava/lang/String;");
        if (env->ExceptionCheck())
            return;
        env->DeleteLocalRef(string_clazz);

        jclass _class_class = env->FindClass("java/lang/Class");
        if (env->ExceptionCheck())
            return;
        class_class = (jclass) env->NewGlobalRef(_class_class);
        env->DeleteLocalRef(_class_class);

        get_classloader_method = env->GetMethodID(class_class, "getClassLoader", "()Ljava/lang/ClassLoader;");
        if (env->ExceptionCheck())
            return;

        jclass _object_class = env->FindClass("java/lang/Object");
        if (env->ExceptionCheck())
            return;
        object_class = (jclass) env->NewGlobalRef(_object_class);
        env->DeleteLocalRef(_object_class);

        get_class_method = env->GetMethodID(object_class, "getClass", "()Ljava/lang/Class;");
        if (env->ExceptionCheck())
            return;

        jclass _classloader_class = env->FindClass("java/lang/ClassLoader");
        if (env->ExceptionCheck())
            return;
        classloader_class = (jclass) env->NewGlobalRef(_classloader_class);
        env->DeleteLocalRef(_classloader_class);

        load_class_method = env->GetMethodID(classloader_class, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        if (env->ExceptionCheck())
            return;

        jclass _no_class_def_found_class = env->FindClass("java/lang/NoClassDefFoundError");
        if (env->ExceptionCheck())
            return;
        no_class_def_found_class = (jclass) env->NewGlobalRef(_no_class_def_found_class);
        env->DeleteLocalRef(_no_class_def_found_class);

        ncdf_init_method = env->GetMethodID(no_class_def_found_class, "<init>", "(Ljava/lang/String;)V");
        if (env->ExceptionCheck())
            return;

        jclass _throwable_class = env->FindClass("java/lang/Throwable");
        if (env->ExceptionCheck())
            return;
        throwable_class = (jclass) env->NewGlobalRef(_throwable_class);
        env->DeleteLocalRef(_throwable_class);

        get_message_method = env->GetMethodID(throwable_class, "getMessage", "()Ljava/lang/String;");
        if (env->ExceptionCheck())
            return;

        init_cause_method = env->GetMethodID(throwable_class, "initCause",
                                            "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
        if (env->ExceptionCheck())
            return;

        jclass _methodhandles_lookup_class = env->FindClass("java/lang/invoke/MethodHandles$Lookup");
        if (env->ExceptionCheck())
            return;
        methodhandles_lookup_class = (jclass) env->NewGlobalRef(_methodhandles_lookup_class);
        env->DeleteLocalRef(_methodhandles_lookup_class);

        lookup_init_method = env->GetMethodID(methodhandles_lookup_class, "<init>", "(Ljava/lang/Class;)V");
        if (env->ExceptionCheck())
            return;

#ifdef USE_HOTSPOT
        jclass _methodhandle_natives_class = env->FindClass("java/lang/invoke/MethodHandleNatives");
        if (env->ExceptionCheck())
            return;
        methodhandle_natives_class = (jclass) env->NewGlobalRef(_methodhandle_natives_class);
        env->DeleteLocalRef(_methodhandle_natives_class);

        link_call_site_method = env->GetStaticMethodID(methodhandle_natives_class, "linkCallSite",
            "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;");
        is_jvm11_link_call_site = false;
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            link_call_site_method = env->GetStaticMethodID(methodhandle_natives_class, "linkCallSite",
                "(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/invoke/MemberName;");
            is_jvm11_link_call_site = true;
            if (env->ExceptionCheck())
                return;
        }
#endif
    }

#ifdef USE_HOTSPOT
    jobject link_call_site(JNIEnv *env, jobject caller_obj, jobject bootstrap_method_obj,
        jobject name_obj, jobject type_obj, jobject static_arguments, jobject appendix_result) {
        if (is_jvm11_link_call_site) {
            return env->CallStaticObjectMethod(methodhandle_natives_class, link_call_site_method, caller_obj, 0,
                bootstrap_method_obj, name_obj, type_obj, static_arguments, appendix_result);
        }
        return env->CallStaticObjectMethod(methodhandle_natives_class, link_call_site_method, caller_obj,
            bootstrap_method_obj, name_obj, type_obj, static_arguments, appendix_result);
    }
#endif

    template <>
    jarray create_array_value<1>(JNIEnv *env, jint size) {
        return env->NewBooleanArray(size);
    }

    template <>
    jarray create_array_value<2>(JNIEnv *env, jint size) {
        return env->NewCharArray(size);
    }

    template <>
    jarray create_array_value<3>(JNIEnv *env, jint size) {
        return env->NewByteArray(size);
    }

    template <>
    jarray create_array_value<4>(JNIEnv *env, jint size) {
        return env->NewShortArray(size);
    }

    template <>
    jarray create_array_value<5>(JNIEnv *env, jint size) {
        return env->NewIntArray(size);
    }

    template <>
    jarray create_array_value<6>(JNIEnv *env, jint size) {
        return env->NewFloatArray(size);
    }

    template <>
    jarray create_array_value<7>(JNIEnv *env, jint size) {
        return env->NewLongArray(size);
    }

    template <>
    jarray create_array_value<8>(JNIEnv *env, jint size) {
        return env->NewDoubleArray(size);
    }

    jobjectArray create_multidim_array(JNIEnv *env, jobject classloader, jint count, jint required_count,
        const char *class_name, int line, std::initializer_list<jint> sizes, int dim_index) {
        if (required_count == 0) {
            env->FatalError("required_count == 0");
            return nullptr;
        }
        jint current_size = sizes.begin()[dim_index];
        if (current_size < 0) {
            throw_re(env, "java/lang/NegativeArraySizeException", "MULTIANEWARRAY size < 0", line);
            return nullptr;
        }
        jobjectArray result_array = nullptr;
        if (count == 1) {
            std::string renamed_class_name(class_name);
            std::replace(renamed_class_name.begin(), renamed_class_name.end(), '/', '.');
            jstring renamed_class_name_string = env->NewStringUTF(renamed_class_name.c_str());
            jclass clazz = find_class_wo_static(env, classloader, renamed_class_name_string);
            env->DeleteLocalRef(renamed_class_name_string);
            if (env->ExceptionCheck()) {
                return nullptr;
            }
            result_array = env->NewObjectArray(current_size, clazz, nullptr);
            if (env->ExceptionCheck()) {
                return nullptr;
            }
            return result_array;
        }
        std::string clazz_name = std::string(count - 1, '[') + "L" + std::string(class_name) + ";";
        if (jclass clazz = env->FindClass(clazz_name.c_str())) {
            result_array = env->NewObjectArray(current_size, clazz, nullptr);
            if (env->ExceptionCheck()) {
                return nullptr;
            }
            env->DeleteLocalRef(clazz);
        } else {
            return nullptr;
        }

        if (required_count == 1) {
            return result_array;
        }

        for (jint i = 0; i < current_size; i++) {
            jobjectArray inner_array = create_multidim_array(env, classloader, count - 1, required_count - 1,
                class_name, line, sizes, dim_index + 1);
            if (env->ExceptionCheck()) {
                env->DeleteLocalRef(result_array);
                return nullptr;
            }
            env->SetObjectArrayElement(result_array, i, inner_array);
            env->DeleteLocalRef(inner_array);
            if (env->ExceptionCheck()) {
                env->DeleteLocalRef(result_array);
                return nullptr;
            }
        }
        return result_array;
    }

    jclass find_class_wo_static(JNIEnv *env, jobject classloader, jstring class_name_string) {
        jclass clazz = (jclass) env->CallObjectMethod(
            classloader,
            load_class_method,
            class_name_string
        );
        if (env->ExceptionCheck()) {
            jthrowable exception = env->ExceptionOccurred();
            env->ExceptionClear();
            jobject details = env->CallObjectMethod(
                exception,
                get_message_method
            );
            if (env->ExceptionCheck()) {
                env->DeleteLocalRef(exception);
                return nullptr;
            }
            jobject new_exception = env->NewObject(no_class_def_found_class,
                ncdf_init_method,
                details);
            if (env->ExceptionCheck()) {
                env->DeleteLocalRef(exception);
                env->DeleteLocalRef(details);
                return nullptr;
            }
            env->CallVoidMethod(new_exception, init_cause_method, exception);
            if (env->ExceptionCheck()) {
                env->DeleteLocalRef(new_exception);
                env->DeleteLocalRef(exception);
                env->DeleteLocalRef(details);
                return nullptr;
            }
            env->Throw((jthrowable) new_exception);
            env->DeleteLocalRef(exception);
            env->DeleteLocalRef(details);
            return nullptr;
        }
        return clazz;
    }

    void throw_re(JNIEnv *env, const char *exception_class, const char *error, int line) {
        jclass exception_class_ptr = env->FindClass(exception_class);
        if (env->ExceptionCheck()) {
            return;
        }
        env->ThrowNew(exception_class_ptr, ("\"" + std::string(error) + "\" on " + std::to_string(line)).c_str());
        env->DeleteLocalRef(exception_class_ptr);
    }

    void bastore(JNIEnv *env, jarray array, jint index, jint value) {
        if (env->IsInstanceOf(array, boolean_array_class))
            env->SetBooleanArrayRegion((jbooleanArray) array, index, 1, (jboolean*) (&value));
        else
            env->SetByteArrayRegion((jbyteArray) array, index, 1, (jbyte*) (&value));
    }

    jbyte baload(JNIEnv *env, jarray array, jint index) {
        jbyte ret_value;
        if (env->IsInstanceOf(array, boolean_array_class))
            env->GetBooleanArrayRegion((jbooleanArray) array, index, 1, (jboolean*) (&ret_value));
        else
            env->GetByteArrayRegion((jbyteArray) array, index, 1, (jbyte*) (&ret_value));
        return ret_value;
    }

    jclass get_class_from_object(JNIEnv *env, jobject object) {
        jobject result_class = env->CallObjectMethod(object, get_class_method);
        if (env->ExceptionCheck()) {
            return nullptr;
        }
        return (jclass) result_class;
    }

    jobject get_classloader_from_class(JNIEnv *env, jclass clazz) {
        jobject result_classloader = env->CallObjectMethod(clazz, get_classloader_method);
        if (env->ExceptionCheck()) {
            return nullptr;
        }
        return result_classloader;
    }

    jobject get_lookup(JNIEnv *env, jclass clazz) {
        jobject lookup = env->NewObject(methodhandles_lookup_class, lookup_init_method, clazz);
        if (env->ExceptionCheck()) {
            return nullptr;
        }
        return lookup;
    }

    void clear_refs(JNIEnv *env, std::unordered_set<jobject> &refs) {
        for (jobject ref : refs)
            if (env->GetObjectRefType(ref) == JNILocalRefType)
                env->DeleteLocalRef(ref);
        refs.clear();
    }

    jstring get_interned(JNIEnv *env, jstring value) {
        jstring result = (jstring) env->CallObjectMethod(value, string_intern_method);
        if (env->ExceptionCheck())
            return nullptr;
        return result;
    }
}