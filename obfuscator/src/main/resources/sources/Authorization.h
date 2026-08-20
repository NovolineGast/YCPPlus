#ifdef YumeCloud_EXPORTS
#define EXPORTS __declspec(dllexport)
#else
#define EXPORTS __declspec(dllimport)
#endif

extern "C" EXPORTS const char* authorization(const char* ApplicationName, const char* serverURL);