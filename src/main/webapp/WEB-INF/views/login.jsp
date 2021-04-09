<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/login" method="post">
	<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
	<input type="hidden" name="loginRedirect" value="${loginRedirect}" />
	<table>
		<tr>
			<td><input type="text" name="id" autofocus="autofocus" placeholder="아이디 입력" required="required"/></td>
		</tr>
		<tr>
			<td><input type="password" name="pw" placeholder="비밀번호 입력" required="required"/></td>
		</tr>
		<tr>
			<td><input type="submit" value="로그인"/></td>
		</tr>
	</table>
</form>
</body>
</html>