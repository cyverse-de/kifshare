const js = require("@eslint/js");
const globals = require("globals");

module.exports = [
  js.configs.recommended,
  {
    files: ["ui/src/js/**/*.js"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "script",
      globals: {
        ...globals.browser,
        jQuery: "readonly",
        $: "readonly",
        Mustache: "readonly",
        _: "readonly",
      },
    },
    rules: {
      strict: ["error", "function"],
    },
  },
];
