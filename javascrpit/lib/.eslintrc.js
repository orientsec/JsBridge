module.exports = {
    'env': {
        'browser': true,
        'es2021': true
    },
    'extends': [
        'airbnb-typescript/base'
    ],
    'parser': '@typescript-eslint/parser',
    'parserOptions': {
        'ecmaVersion': 'latest',
        'sourceType': 'module',
        'parser': '@typescript-eslint/parser',
        'project': './tsconfig.json',
        'tsconfigRootDir': __dirname
    },
    'plugins': [
        '@typescript-eslint',
        'import'
    ],
    'rules': {
        '@typescript-eslint/indent': ['error', 4],
        '@typescript-eslint/semi': ['error', 'never'],
        '@typescript-eslint/comma-dangle': 'off',
        'linebreak-style': ['error', 'windows'],
        'quotes': ['error', 'single'],
        'semi': ['error', 'never'],
        'max-classes-per-file': 'off',
        '@typescript-eslint/no-non-null-assertion': 'error',
        'import/extensions': [
            'error',
            'ignorePackages',
            {
                'js': 'never',
                'jsx': 'never',
                'ts': 'never',
                'tsx': 'never'
            }
        ]
    }
}
