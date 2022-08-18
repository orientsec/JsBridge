const path = require('path')
const HtmlWebpackPlugin = require('html-webpack-plugin')

const config = {
    devServer: {
        hot: true
    },
    entry: {
        index: './src/index.tsx'
    },
    devtool: 'eval-source-map',
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
        extensions: ['.tsx', '.ts', '.js', '.d.ts']
    },
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: '[name].js'
    },
    plugins: [
        new HtmlWebpackPlugin({
            title: '首页',
            filename: 'index.html',
            template: './resources/index.html'
        })
    ],
    mode: 'development'
}

module.exports = config