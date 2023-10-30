package com.wiseasy.ecr.hub.sdk;

import com.fazecast.jSerialComm.SerialPort;

public class ECRHubConfig {
    /**
     * Host name
     */
    private String hostName;
    /**
     * Host alias name
     */
    private String aliasName;
    /**
     * Serial Port Configuration
     */
    private SerialPortConfig serialPortConfig = new SerialPortConfig();
    /**
     * Socket Configuration
     */
    private SocketConfig socketConfig = new SocketConfig();

    public ECRHubConfig() {
    }

    public ECRHubConfig(String hostName, String aliasName) {
        this.hostName = hostName;
        this.aliasName = aliasName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public SerialPortConfig getSerialPortConfig() {
        return serialPortConfig;
    }

    public void setSerialPortConfig(SerialPortConfig serialPortConfig) {
        this.serialPortConfig = serialPortConfig;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }


    /**
     * Serial Port Configuration
     */
    public static class SerialPortConfig {
        /**
         * SerialPort name tag
         */
        private String portNameKeyword = "GPS";
        /**
         * Baud rate
         */
        private int baudRate = 115200;
        /**
         * Data bits
         */
        private int dataBits = 8;
        /**
         * Stop bits (1 stop bit = 1, 1.5 stop bits = 2, 2 stop bits = 3)
         */
        private int stopBits = 1;
        /**
         * Checksum mode (no checksum = 0, odd checksum = 1, even checksum = 2, mark checksum = 3, space checksum = 4)
         */
        private int parity = 0;
        /**
         * Timeout mode
         * Non-blocking mode: #TIMEOUT_NONBLOCKING [In this mode, readBytes(byte[], long) and writeBytes(byte[], long) calls return any available data immediately.]
         * Write Blocking Mode: #TIMEOUT_WRITE_BLOCKING [In this mode, the writeBytes(byte[], long) call will block until all data bytes have been successfully written to the output serial device].
         * Semi-blocking read mode: #TIMEOUT_READ_SEMI_BLOCKING [In this mode, readBytes(byte[], long) call will block until the specified timeout is reached or at least 1 byte of data can be read].
         * Full blocking read mode: #TIMEOUT_READ_BLOCKING [In this mode, the readBytes(byte[], long) call will block until the specified timeout is reached or until at least 1 byte can be returned as requested].
         * Scanner mode: #TIMEOUT_SCANNER [This mode applies to reading from the serial port using Java's java.util.Scanner class, and ignores manually specified timeout values to ensure compatibility with the Java specification].
         */
        private int timeoutMode = SerialPort.TIMEOUT_READ_SEMI_BLOCKING;
        /**
         * Write timeout (milliseconds)
         */
        private int writeTimeout = 10 * 1000;
        /**
         * Read timeout (milliseconds)
         */
        private int readTimeout = 180 * 1000;
        /**
         * Connection timeout (milliseconds)
         */
        private int connTimeout = 10 * 1000;

        public String getPortNameKeyword() {
            return portNameKeyword;
        }

        public void setPortNameKeyword(String portNameKeyword) {
            this.portNameKeyword = portNameKeyword;
        }

        public int getBaudRate() {
            return baudRate;
        }

        public void setBaudRate(int baudRate) {
            this.baudRate = baudRate;
        }

        public int getDataBits() {
            return dataBits;
        }

        public void setDataBits(int dataBits) {
            this.dataBits = dataBits;
        }

        public int getStopBits() {
            return stopBits;
        }

        public void setStopBits(int stopBits) {
            this.stopBits = stopBits;
        }

        public int getParity() {
            return parity;
        }

        public void setParity(int parity) {
            this.parity = parity;
        }

        public int getTimeoutMode() {
            return timeoutMode;
        }

        public void setTimeoutMode(int timeoutMode) {
            this.timeoutMode = timeoutMode;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public int getConnTimeout() {
            return connTimeout;
        }

        public void setConnTimeout(int connTimeout) {
            this.connTimeout = connTimeout;
        }
    }

    /**
     * Socket Configuration
     */
    public static class SocketConfig {
        /**
         * Write timeout (milliseconds)
         */
        private int writeTimeout = 10 * 1000;
        /**
         * Read timeout (milliseconds)
         */
        private int readTimeout = 180 * 1000;
        /**
         * Connection timeout (milliseconds)
         */
        private int connTimeout = 30 * 1000;

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getConnTimeout() {
            return connTimeout;
        }

        public void setConnTimeout(int connTimeout) {
            this.connTimeout = connTimeout;
        }
    }
}