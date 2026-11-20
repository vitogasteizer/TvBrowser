# Adding a language

**English** · [ქართული](TRANSLATING.ka.md)

All app texts are in separate language files, so adding a language needs no code changes.

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-XX/strings.xml`, where `XX` is the language code (for example `values-de`, `values-es`, `values-fr`).
2. Translate the text between the `<string>` tags. Do not change the `name` attributes and keep placeholders such as `%s`, `%d` and `%1$d`. Use `\n` for a line break and `\'` for an apostrophe.
3. Add your language to `app/src/main/res/values/arrays.xml` (a code in `lang_codes` and its own name in `lang_names`) so it appears in **Settings → Language**.
4. Set `lang_code` to your code (it is the `lang` of the phone remote page).
5. The strings that start with `remote_` are the texts of the phone remote page. They live in the same file.
6. By default the app picks the language of the TV automatically and falls back to English.

For the documentation, copy `README.md` to `README.XX.md` and `docs/SETUP.md` to `docs/SETUP.XX.md`, translate them, and add a link to the language line at the top of each file. Screenshots for your language can go to `docs/images/XX/` (optional).
