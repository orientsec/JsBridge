/**
 * 日志级别枚举
 */
export enum LogLevel {
    INFO = 'info',
    WARN = 'warn',
    ERROR = 'error'
}

/**
 * 日志记录器类
 */
class Logger {
    /**
     * 日志标签
     */
    private readonly TAG = '[JsBridge]'

    /**
     * 是否可调试
     */
    debuggable = false

    /**
     * 格式化日志消息
     * @param level 日志级别
     * @param message 日志消息
     * @returns 格式化后的消息
     */
    private formatMessage(level: LogLevel, message: string): string {
        const timestamp = new Date().toISOString()
        return `${this.TAG} ${timestamp} [${level}] ${message}`
    }

    /**
     * 记录信息日志
     * @param message 日志消息
     */
    info(message: string): void {
        if (this.debuggable) {
            console.info(this.formatMessage(LogLevel.INFO, message))
        }
    }

    /**
     * 记录警告日志
     * @param message 日志消息
     */
    warn(message: string): void {
        if (this.debuggable) {
            console.warn(this.formatMessage(LogLevel.WARN, message))
        }
    }

    /**
     * 记录错误日志
     * @param message 日志消息
     * @param error 错误对象
     */
    error(message: string, error?: unknown): void {
        if (this.debuggable) {
            const formattedMessage = this.formatMessage(LogLevel.ERROR, message)
            if (error) {
                console.error(formattedMessage, error)
            } else {
                console.error(formattedMessage)
            }
        }
    }
}

/**
 * 导出单例日志记录器实例
 */
export const logger = new Logger()
