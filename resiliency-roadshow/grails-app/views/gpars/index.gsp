<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Resiliency Test Suite</title>
    <asset:javascript src="spring-websocket" />
    <asset:javascript src="console.js" />
    <asset:stylesheet href="console.css"/>
    <script type="text/javascript">
        $(function() {

            var logConsole = new SumoConsole(document.getElementById('logElem'), {
                limit: 3
            }).init();

            var socket = new SockJS("${createLink(uri: '/stomp')}");
            var client = Stomp.over(socket);

            client.connect({}, function() {
                client.subscribe("/topic/log", function(message) {
                    // logConsole.log(message.body);
                    logConsole.log(message.body.replace(/\"/g, "")); // TODO message comes with double double quotes. Open ticket with Spring!
                });
                client.subscribe("/topic/error", function(message) {
                    // logConsole.error(message.body);
                    logConsole.error(message.body.replace(/\"/g, ""));
                });
            });

            $("#cleanButton").click(function() {
                logConsole.clean();
            });
        });
    </script>
    <style scoped>
        .console-section
        {
            margin: 0 auto 15px auto;
            padding: 10px;
            border: 1px solid rgba(0,0,0,0.05);
            background-color: rgba(0,0,0,0.01)
        }

        .console-section > .title {
            margin: 5px 0 15px 0;
            text-align: center;
        }

        .console-section p a.hyperlink,
        .config-section a {
            text-decoration: none;
        }
    </style>
</head>
</head>
<body>
<a href="#list-actions" class="skip" tabindex="-1">
    <g:message ode="default.link.skip.label" default="Skip to content&hellip;" />
</a>
<div class="nav" role="navigation">
    <ul>
        <li>
            <g:link class="home"><g:message code="default.home.label" /></g:link>
        </li>
        <li>
            <g:link class="list" controller="circuitBreaker">Circuit Breakers</g:link>
        </li>
        <li>
            <g:link class="list" controller="gpars">GPars</g:link>
        </li>
    </ul>
</div>
<div id="list-actions" class="content scaffold-list" role="main">
    <h1>
        Resiliency Test Suite
    </h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">
            ${flash.message}
        </div>
    </g:if>
    <table>
        <thead>
        <tr>

            <g:sortableColumn property="action" title="action" />
            <g:sortableColumn property="result" title="result" />

        </tr>
        </thead>
        <tbody>
        <g:each  var="action" in="${actions}" status="i">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    <g:remoteLink 	action="${action}"
                                     params="[a: 'Sumo']"
                                     update='[success:"${action}_result",failure:"${action}_result"]'
                                     on401="showLogin();"
                                     onFailure="if (XMLHttpRequest.status==401) showLogin();">${action}
                    </g:remoteLink>
                <td id="${action}_result"></td>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>

<div class="console-section">
    <h1 class="title">Server Log <button style="float:right;" id="cleanButton">Clean Console</button></h1>
    <div id="logElem" class="console" data-console-options='{"limit":10}'></div>
</div>

</body>
</html>
