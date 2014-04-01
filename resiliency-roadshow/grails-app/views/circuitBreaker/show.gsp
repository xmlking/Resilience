<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Resiliency Test Suite</title>
		<style type="text/css" media="screen">
			.spinner {
			    position: fixed;
			    top: 50%;
			    left: 50%;
			    margin-left: -50px; /* half width of the spinner gif */
			    margin-top: -50px; /* half height of the spinner gif */
			    text-align:center;
			    z-index:1234;
			    overflow: auto;
			    width: 100px; /* width of the spinner gif */
			    height: 102px; /*hight of the spinner gif +2px to fix IE8 issue */
			}

			
		</style>
	</head>

	<body>
		<a href="#list-actions" class="skip" tabindex="-1">
			<g:message ode="default.link.skip.label" default="Skip to content&hellip;" />
		</a>
		<div class="nav" role="navigation">
			<ul>
				<li>
					<g:link class="home" controller="demo"><g:message code="default.home.label" /></g:link>					
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
			<g:if test="${params.id}">
				<h1>Circuit Breaker: ${params.id}</h1>
			</g:if>
			<g:if test="${flash.message}">
				<div class="message" role="status">
					${flash.message}
				</div>
			</g:if>			
			<g:if test="${error}">
				<div class="error" role="status">
					${error.message} (view source for stack trace)
					<div style='display:none'>
						<%
						def stack = new StringWriter()
						error.printStackTrace(new PrintWriter(stack))
						%>
						${stack?.encodeAsHTML()}
					</div>
				</div>
			</g:if>
			<g:if test="${circuitBreaker}">
			<table>
				<tr>
					<td>name</td>
					<td>${circuitBreaker.name}</td>
				</tr>
				<tr>
					<td>failureThreshold</td>
					<td>${circuitBreaker.failureThreshold}</td>
				</tr>
				<tr>
					<td>failureThresholdTimeFrameMs</td>
					<td>${circuitBreaker.failureThresholdTimeFrameMs}</td>
				</tr>
				<tr>
					<td>retryAfterMs</td>
					<td>${circuitBreaker.retryAfterMs}</td>
				</tr>
				<tr>
					<td>failureIndications</td>
					<td>${circuitBreaker.failureIndications}</td>
				</tr>
				<tr>
					<td>status</td>
					<td>${circuitBreaker.status}</td>
				</tr>		
				<tr>
					<td>failures</td>
					<td>${circuitBreaker.failures}</td>
				</tr>
				<tr>
					<td>closedCycleCounter</td>
					<td>${circuitBreaker.closedCycleCounter}</td>
				</tr>
				<tr>
					<td>lastOpenedTime</td>
					<td>${circuitBreaker.lastOpenedTime}</td>
				</tr>	
			</table>
			</g:if>
			<g:else>
				There is no Circuit Breaker with name: ${params.id} found. 
			</g:else>
		</div>
	</body>
</html>