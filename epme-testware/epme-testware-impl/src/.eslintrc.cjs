module.exports = {
  extends: ['@open-wc/eslint-config', 'eslint-config-prettier'],
  parser: '@babel/eslint-parser',
  parserOptions: {
    requireConfigFile: false,
  },
  globals: {
    __ENV: true,
  },
  rules: {
    // Tries to force static, static methods can't be overridden when extending class
    'class-methods-use-this': 0,
    'import/no-unresolved': 0,
    // reassigning CustomEvent detail is common pattern
    'no-param-reassign': 0,
    'lit-a11y/click-events-have-key-events': 0,
    // as project is small, many files that will export multiple only export one thing
    'import/prefer-default-export': 0,
    'max-classes-per-file': 0,
    'no-restricted-syntax': 0,
    'import/named': 0,
    'no-restricted-globals': 0,
    'comma-dangle': [
      'error',
      {
        arrays: 'always-multiline',
        objects: 'always-multiline',
        imports: 'always-multiline',
        exports: 'always-multiline',
        functions: 'always-multiline',
      },
    ],
    'import/no-import-module-exports': [
      'error',
      {
        exceptions: [
          '**/*/gateway-tests.js',
          '**/*/epme-tests.js',
          '**/*/epme-app-staging-tests.js',
          '**/*/pmsch.js',
          '**/*/epme.js',
          '**/*/dmm.js',
          '**/*/common.js',
          '**/*/api-gateway.js',
          '**/*/epme-database.js',
          '**/*/rbac-tests.js',
          '**/*/rbac.js',
          '**/*/kafka.js',
        ],
      },
    ],
  },
};
