<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>title</title>
</head>
	<body>
	   <tiles:insertAttribute name="body" ignore="true"/>
	   <hr/>
	   <tiles:insertAttribute name="footer"/> <%--  value="/WEB-INF/views/common/layout/footer.jsp" --%>
	</body>
</html>