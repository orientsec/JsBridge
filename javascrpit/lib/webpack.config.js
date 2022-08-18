const path = require('path')
const TerserPlugin = require("terser-webpack-plugin");

const config = {
    entry: {
        index: './src/index.ts'
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/
            }
        ]
    },
    resolve: {
        extensions: ['.ts', '.d.ts', '.js']
    },
    output: {
        path: path.resolve(__dirname, './dist'),
        filename: '[name].min.js',
        library: 'JsBridge',
        libraryExport: ['default'],
        libraryTarget: 'umd'
    },
    mode: 'production',
    // mode: 'development',
    optimization: {
        minimizer: [
            new TerserPlugin({
                include: [/\.min\.js$/]
            }),
        ]
    }
}

module.exports = config