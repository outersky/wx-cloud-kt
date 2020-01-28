package cn.hillwind.wx.cloud

interface WxCloudApiUrl {
    /**
     * 得到api完整地址.
     *
     * @return api地址
     */
    fun getUrl(): String

    //云数据库
    enum class Database(val prefix: String, val path: String) : WxCloudApiUrl {
        /**
         * databasequery.
         */
        QUERY(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/databasequery"),
        /**
         * databasecount.
         */
        COUNT(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/databasecount"),
        /**
         * databaseupdate.
         */
        ADD(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/databaseadd"),

        /**
         * databaseupdate.
         */
        UPDATE(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/databaseupdate");

        override fun getUrl(): String {
            return WxCloudHostConfig.buildUrl(prefix, path)
        }
    }

    //云存储
    enum class Storage(val prefix: String, val path: String) : WxCloudApiUrl {

        /**
         * uploadfile.
         */
        UPLOAD(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/uploadfile"),

        /**
         * batchdownloadfile.
         */
        DOWNLOAD(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/batchdownloadfile"),

        /**
         * batchdeletefile.
         */
        DELETE(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/batchdeletefile");

        override fun getUrl(): String {
            return WxCloudHostConfig.buildUrl(prefix, path)
        }
    }

    //云函数
    enum class Function(val prefix: String, val path: String) : WxCloudApiUrl {
        /**
         * invokecloudfunction.
         */
        INVOKE(WxCloudHostConfig.API_DEFAULT_HOST_URL, "/tcb/invokecloudfunction");

        override fun getUrl(): String {
            return WxCloudHostConfig.buildUrl(prefix, path)
        }
    }

}