# Native layer: `libvkx.so` (Ghidra)

Артефакт: `re/decoded/lib/arm64-v8a/libvkx.so`
Пакет приложения: `ua.itaysonlab.vkx`, версия `8.14.2_pub` (`100150`).

## Что это за библиотека

`libvkx.so` — AArch64 ELF, stripped, 1 805 264 байт. Внутри:

- libsodium (экспортирует `sodium_*`, `crypto_*`, `randombytes_*`);
- `JNI_OnLoad` по адресу `0xa5b18`;
- native-реализация `ua.itaysonlab.vkxnative.VKXNative`.

Это **не сетевой слой VK** и не клиент API. Библиотека занимается
anti-tamper/anti-hook проверками и вспомогательной криптографией для
silent-auth. Для нового backend она не нужна.

## JNI-регистрация

`JNI_OnLoad` вызывает `RegisterNatives` для класса
`ua/itaysonlab/vkxnative/VKXNative` с тремя методами:

| Java-метод | Сигнатура | Native-функция |
|---|---|---|
| `x00` | `()Lua/itaysonlab/vkxnative/BundleNativeClass;` | `FUN_0x88248` |
| `x01` | `()Lua/itaysonlab/vkxnative/BundleNativeClass;` | `FUN_0x88248` |
| `x02` | `(Ljava/lang/String;)Lua/itaysonlab/vkxnative/BundleNativeClass;` | `FUN_0xa4ed0` |

`x00` и `x01` — один и тот же код. Они возвращают `BundleNativeClass`
с массивом строк; в таблице видно 16 имён известных hook/распаковщиков:

```
de/robv/android/xposed/XposedBridge
de/robv/android/xposed/XposedHelpers
Dexunpacker/Dump
Dexunpacker/AppEntry
Dexunpacker/AppStubLoader
Dexunpacker/HiddenApiBypass2
fpa/core/FPAInit
fpa/core/CoreEntry
fpa/core/NativeCalls
fpa/core/signbypass/PMSignBypass
fpa/core/entry/NativeHookBridge
fpa/hook/FPAHelper
fun/fpa/wrapper/WrapperBase
org/lsposed/hidden/HiddenApiBypass
top/canyie/pine/Pine
org/lsposed/lsplant/LSPlant
```

В коде видны reflection-вызовы `ClassLoader.loadClass`, `Field.set`,
строки `disableHooks`, `TRUE`/`FALSE`. То есть проверка пытается
обнаружить и, вероятно, отключить hook-фреймворки.

## `x02` / silent auth

`FUN_0xa4ed0`:

- берёт входную строку из Java (`GetStringUTFChars`);
- использует строковый ключ `THETRUTHLIES`;
- вызывает `FUN_0x1a4bb4` и `FUN_0x1a5640` (криптографические преобразования);
- возвращает `BundleNativeClass` с `Object[1]`, где лежит вычисленный `code`.

Этот код используется в `defpackage/C14079z` после VK-метода `apps.get`:
результат уходит в `Collections.singletonMap("code", ...)` для silent-auth.
Для нашей авторизации (email/password + JWT) этот механизм не нужен.
Алгоритм x02 не переиспользуем и VK-токены не извлекаем.

## Как воспроизвести

```bash
export JAVA_HOME=/tmp/opencode/jdk25
/tmp/opencode/ghidra_12.2_PUBLIC/support/analyzeHeadless \
  /tmp/opencode/ghidra_proj vkx \
  -import re/decoded/lib/arm64-v8a/libvkx.so \
  -overwrite \
  -scriptPath /tmp/opencode/ghidra_scripts \
  -postScript DumpJNI.java
```

Ghidra не декомпилирует DEX. Для Java-кода используется JADX, см.
`tools/decompile.sh` и `re/README.md`.

## Вывод для проекта

- Портировать `libvkx.so` не нужно.
- `VKXNative`, проверки hook-фреймворков и silent-auth вырезаются вместе
  с VK-сетевым кодом.
- Наша авторизация — собственный `/auth/login` + JWT, без native-зависимостей.
