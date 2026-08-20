// VMProtect SDK 兼容桩（开源版）
//
// 本仓库不随源码分发商业版 VMProtect SDK 与 vmp.exe。此头文件提供与官方
// VMProtectSDK.h 同名 API 的空实现，其行为与"未经 VMProtect 处理的官方 SDK"
// 完全一致：保护标记为空操作，字符串解密原样返回，检测函数恒返回安全值。
//
// 如需启用真实的 VMProtect 保护：购买授权后用官方 SDK 的 VMProtectSDK.h
// 覆盖本文件，并将 VMProtectSDK64.lib、VMProtectSDK64.dll、vmp.exe 及
// YumeCloud_NativeLibrary.vmp 工程文件放入 src/main/resources/sources/。
// 构建流程会自动检测这些文件：存在时链接 SDK 并执行加壳，缺失时跳过。
#pragma once

#ifdef __cplusplus
extern "C" {
#endif

// protection markers: no-op without VMProtect processing
static inline void VMProtectBegin(const char *marker) { (void) marker; }
static inline void VMProtectBeginVirtualization(const char *marker) { (void) marker; }
static inline void VMProtectBeginMutation(const char *marker) { (void) marker; }
static inline void VMProtectBeginUltra(const char *marker) { (void) marker; }
static inline void VMProtectBeginVirtualizationLockByKey(const char *marker) { (void) marker; }
static inline void VMProtectBeginUltraLockByKey(const char *marker) { (void) marker; }
static inline void VMProtectEnd(void) {}

// utils: pass-through without VMProtect processing
static inline int VMProtectIsProtected(void) { return 0; }
static inline int VMProtectIsDebuggerPresent(int detectVirtualPC) { (void) detectVirtualPC; return 0; }
static inline int VMProtectIsVirtualMachinePresent(void) { return 0; }
static inline int VMProtectIsValidImageCRC(void) { return 1; }
static inline const char *VMProtectDecryptStringA(const char *value) { return value; }
#ifdef _WIN32
static inline const wchar_t *VMProtectDecryptStringW(const wchar_t *value) { return value; }
#else
static inline const unsigned short *VMProtectDecryptStringW(const unsigned short *value) { return value; }
#endif
static inline int VMProtectFreeString(const void *value) { (void) value; return 1; }

#ifdef __cplusplus
}
#endif
