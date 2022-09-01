//
// Created by beich on 2021/2/19.
//

#include "hook_env.h"

FUN_INTERCEPT HOOK_DEF(char*, getenv, const char *name) {
    char *value = get_orig_getenv()(name);
    LOGV("getenv name: %s, value: %s", name, value);
    if (value == nullptr) {
        return value;
    }
    char *result = FXHandler::EnvironmentReplace(name, value);
    LOGV("getenv name: %s, value: %s, replaced value: %s", name, value, result);
    return result == nullptr ? value : result;
}
