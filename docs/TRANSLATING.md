# Adding a language

**English** · [ქართული](TRANSLATING.ka.md)

All app texts are in separate language files, so adding a language needs no code changes.

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-XX/strings.xml`, where `XX` is the language code (for example `values-de`, `values-es`, `values-fr`).
2. Translate the text between the `<string>` tags. Do not change the `name` attributes and keep placeholders such as `%s`, `%d` and `%1$d`. Use `\n` for a line break and `\'` for an apostrophe.
3. Set `lang_code` to your code (it is the `lang` of the phone remote page).
4. The strings that start with `remote_` are the texts of the phone remote page. They live in the same file.
5. The app picks the language of the TV automatically and falls back to English.

For the documentation, copy `README.md` to `README.XX.md` and `docs/SETUP.md` to `docs/SETUP.XX.md`, translate them, and add a link to the language line at the top of each file. Screenshots for your language can go to `docs/images/XX/` (optional).
