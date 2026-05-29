import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'

export default [
  { ignores: ['dist/', 'node_modules/'] },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    languageOptions: {
      globals: {
        window: 'readonly',
        document: 'readonly',
        console: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        requestAnimationFrame: 'readonly',
        FileReader: 'readonly',
        URL: 'readonly',
        Event: 'readonly',
        Element: 'readonly',
        SVGElement: 'readonly',
        MathMLElement: 'readonly',
        localStorage: 'readonly',
        fetch: 'readonly',
        navigator: 'readonly',
        location: 'readonly',
        history: 'readonly',
        FormData: 'readonly',
        Blob: 'readonly',
        File: 'readonly',
        Image: 'readonly',
        HTMLElement: 'readonly',
        HTMLInputElement: 'readonly',
        HTMLTextAreaElement: 'readonly',
      },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'no-unused-vars': ['error', { caughtErrors: 'none' }],
    },
  },
  // Test files: add Vitest/Node globals
  {
    files: ['src/__tests__/**', '**/*.test.js', '**/*.spec.js'],
    languageOptions: {
      globals: {
        global: 'readonly',
        vi: 'readonly',
        describe: 'readonly',
        it: 'readonly',
        expect: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        beforeAll: 'readonly',
        afterAll: 'readonly',
        jest: 'readonly',
      },
    },
    rules: {
      'no-unused-vars': ['error', { caughtErrors: 'none', varsIgnorePattern: '^_' }],
    },
  },
]
