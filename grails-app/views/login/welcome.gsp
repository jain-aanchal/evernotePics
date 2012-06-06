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
    <title>Welcome !!</title>
    <meta name="layout" content="main">
    <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}" />
    <link rel="stylesheet" href="${resource(dir:'css',file:'welcome.css')}" />
    <link rel="stylesheet" href="${resource(dir:'css',file:'jquery.lightbox-0.5.css')}" />
    <script type="text/javascript" src=${resource(dir:'js',file:'jquery.js')}></script>
    <script type="text/javascript" src=${resource(dir:'js',file:'jquery.lightbox-0.5.js')}></script>
    <script type="text/javascript" src=${resource(dir:'js',file:'main.js')}> </script>


</head>
<body>


<div class="content">


    <ul id="gallery" class="images-list">


        <%
            for(String s : params.url){

        %>
        <li class="image">

            <a href="${resource(dir:'images',file:"${s}")}" >

                <img width="256" height="256" src=${resource(dir:'images',file:"${s}")}  />

            </a>
        </li>
        <%
            }

        %>
    </ul>
</div>
</body>
</html>