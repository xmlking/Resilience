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
			<h1>
				Circuit Breakers
			</h1>
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
			<g:if test="${circuitBreakersMap}">
			<table>
				<tr>
					<th>Name</th>
					<th>Failure Threshold</th>
					<th>Failure Threshold Time Frame Ms</th>
					<th>Retry After Ms</th>
					<th>Status</th>
					<th>Failures</th>
					<th>Closed Cycle Counter</th>
					<th>Last Opened Time</th>
				</tr>
				<g:each var='circuitBreakerBeanName' in="${circuitBreakersMap.keySet().sort()}">
					<g:set var='circuitBreaker' value="${circuitBreakersMap[circuitBreakerBeanName]}" />
					<tr>
						<td>
							<g:link action='show' params="[cb_name: circuitBreaker.name]">${circuitBreaker.name.substring(circuitBreaker.name.indexOf('(')-20 , circuitBreaker.name.indexOf(')')+1)}
							</g:link>
						</td>
						<td>${circuitBreaker.failureThreshold}</td>
						<td>${circuitBreaker.failureThresholdTimeFrameMs}</td>
						<td>${circuitBreaker.retryAfterMs}</td>
						<td>${circuitBreaker.status}</td>
						<td>${circuitBreaker.failures}</td>
						<td>${circuitBreaker.closedCycleCounter}</td>
						<td>${circuitBreaker.lastOpenedTime}</td>
					</tr>
				</g:each>
			</table>
			</g:if>
			<g:else>
				There are no active Circuit Breakers.
			</g:else>
		</div>
	</body>
</html>