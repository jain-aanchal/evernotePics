<%--
  Created by IntelliJ IDEA.
  User: aanchal
  Date: 6/2/12
  Time: 3:21 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title></title>
    <meta name="layout" content="main">
    <link rel="stylesheet" type="text/css" href="/css/main.css" />
    <link rel="stylesheet" type="text/css" href="/css/welcome.css" />
    <link rel="stylesheet" type="text/css" href="/css/jquery.lightbox-0.5.css" />
    <script type="text/javascript" src="/js/jquery.js"></script>
    <script type="text/javascript" src="/js/jquery.lightbox-0.5.js"></script>


    <script type="text/javascript" src="/js/main.js"> </script>


</head>
<body>


<div class="content">


    <ul id="gallery" class="images-list">


        <%
            for(String s : params.url){

        %>
        <li class="image">
            <a href="<%= s  %>">
                <img width="256" height="256" src="<%= s  %>"/>
            </a>
        </li>
        <%
            }

        %>
    </ul>
</div>
</body>
</html>