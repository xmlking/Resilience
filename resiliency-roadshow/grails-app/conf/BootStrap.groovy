import com.crossbusiness.resiliency.demo.WebLogAppender

class BootStrap {
    def brokerMessagingTemplate

    def init = { servletContext ->
        WebLogAppender.brokerMessagingTemplate = brokerMessagingTemplate
        WebLogAppender.appInitialized = true
    }
    def destroy = {
    }
}
