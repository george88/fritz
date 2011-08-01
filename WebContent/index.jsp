<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="georg.Fritz"%>
<% 

	if(request.getAttribute("index.jsp") == null || (request.getAttribute("index.jsp") != null && !request.getAttribute("index.jsp").equals("index.jsp")))
		response.sendRedirect("Fritz");
%>
<%@page import="com.dropbox.client.DropboxAPI.Entry"%>
<%@page import="java.util.ArrayList"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link rel="stylesheet" type="text/css" href="css/styles.css" media="screen" />
	<script type="text/javascript">
    
		var TCNDDU = TCNDDU || {};
		
		(function(){
			var dropContainer,
				dropListing;
			
			TCNDDU.setup = function () {
				dropListing = document.getElementById("output-listing01");
				dropContainer = document.getElementById("output");
				
				dropContainer.addEventListener("dragenter", function(event){dropListing.innerHTML = '';event.stopPropagation();event.preventDefault();}, false);
				dropContainer.addEventListener("dragover", function(event){event.stopPropagation(); event.preventDefault();}, false);
				dropContainer.addEventListener("drop", TCNDDU.handleDrop, false);
			};
			
			TCNDDU.uploadProgressXHR = function (event) {
				if (event.lengthComputable) {
					var percentage = Math.round((event.loaded * 100) / event.total);
					if (percentage < 100) {
						event.target.log.firstChild.nextSibling.firstChild.style.width = (percentage*2) + "px";
						event.target.log.firstChild.nextSibling.firstChild.textContent = percentage + "%";
					}
				}
			};
			
			TCNDDU.loadedXHR = function (event) {
				var currentImageItem = event.target.log;
				
				currentImageItem.className = "loaded";
				console.log("xhr upload of "+event.target.log.id+" complete");
			};
			
			TCNDDU.uploadError = function (error) {
				console.log("error: " + error);
			};
			
			TCNDDU.processXHR = function (file, index) {
				var xhr = new XMLHttpRequest(),
					container = document.getElementById("item"+index),
					fileUpload = xhr.upload,
					progressDomElements = [
						document.createElement('div'),
						document.createElement('p')
					];

				progressDomElements[0].className = "progressBar";
				progressDomElements[1].textContent = "0%";
				progressDomElements[0].appendChild(progressDomElements[1]);
				
				container.appendChild(progressDomElements[0]);
				
				fileUpload.log = container;
				fileUpload.addEventListener("progress", TCNDDU.uploadProgressXHR, false);
				fileUpload.addEventListener("load", TCNDDU.loadedXHR, false);
				fileUpload.addEventListener("error", TCNDDU.uploadError, false);

				xhr.open("POST", "Fritz");
				xhr.overrideMimeType('text/plain; charset=x-user-defined-binary');
                xhr.setRequestHeader("X-File-Name", file.name);
                xhr.setRequestHeader("X-File-Size", file.size);
				xhr.sendAsBinary(file.getAsBinary());
			};
			
			TCNDDU.handleDrop = function (event) {
				var dt = event.dataTransfer,
					files = dt.files,
					imgPreviewFragment = document.createDocumentFragment(),
					count = files.length,
					domElements;
					
				event.stopPropagation();
				event.preventDefault();
                
                var p=1;
				for (var i = 0; i < count; i++) {
					domElements = [
						document.createElement('li'),
						document.createElement('a'),
						document.createElement('img')
					];
					domElements[2].src =  'fotos/0'+p+'.jpg';///files[i].getAsDataURL(); // base64 encodes local file(s)
					domElements[2].width = 300;
					domElements[2].height = 200;
					domElements[1].appendChild(domElements[2]);
					domElements[0].id = "item"+i;
					domElements[0].appendChild(domElements[1]);
					
					imgPreviewFragment.appendChild(domElements[0]);
					
					dropListing.appendChild(imgPreviewFragment);
					
					TCNDDU.processXHR(files.item(i), i);
                    
                    p++;
                    if(p > 9)
                        p=1;
				}
			};
			
			window.addEventListener("load", TCNDDU.setup, false);
		})();
	</script>
	
	<title>F&uuml;r Waldemar</title>

</head>
<body>
	<div id="output" class="clearfix">
		<ul id="output-listing01"></ul>
	</div>
	<%
		Object fl=request.getAttribute("filelist");
        ArrayList<Entry> files = fl!=null?(ArrayList<Entry>)fl:null;
		String dir="";
		long size=0L;
		if(files!=null)
		for(Entry file:files){ size+=file.bytes;}
    %>
    <br />
    <input type="file" name="file" multiple="multiple" />
    <table><tr>
    	<td>
		    <div><%=size/1000000 %> MB verbraucht!</div>
		    <div><%=files!=null?""+files.size():"" %> Lieder vorhanden.</div>
   		</td>
   		<td>
   			<div style="background-color: blue;" ><a href="?podcast=show" target="_blank" ><span><img alt="createPodcast" src="bilder/playlist.png" height="50px" style="float: inherit;" ></span></a></div>
   		</td>
   		<td>
   			<div style="background-color: blue;" ><a href="?stream=ON" target="_blank" ><span><img alt="createm3u" src="bilder/webradio.png" height="50px" style="float: inherit;" ></span></a></div>
   		</td>
   		<td>
   			<div style="background-color: blue;" ><a href="?reset=config"><span><img alt="resetConf" src="bilder/reset.png" height="50px" style="float: inherit;" ></span></a></div>
   		</td>
    </tr></table>
    <div style="background: yellow; overflow: auto; font-weight: bold; border: 2px dotted #fff;-moz-border-radius: 15px; padding: 0px 0px; position: relative; width: 95%; text-align: center;  height: 250px; top: 25px;">
	   <table>
       <%
            int i=0;
            out.print("<tr>");
            if(files!=null)
            for(Entry file:files){ String[]id3=new String[]{file.fileName(),""};//getfromTagMp3('./../files/'.dir.'/'.file) 
       			size+=file.bytes;
       %>
       <!--<embed src="<%="?id="+file.fileName() %>" width="80px" height="20px" autoload="false" autostart="false"></embed>-->
       <!--<a href="<%="?id="+file %>" target="_blank"  ><img src="play.png" id="<%=file.fileName() %>"  /></a>-->
       <!--<a href="javascript:play('<%=Fritz.www_path+"?id="+file %>');"  ><img src="play.png" id="<%=file.fileName() %>"  /></a>-->   
                <td>
                	<div style="background: aqua; position: relative; height: 70px; border: 2px dashed #000000;	-moz-border-radius: 15px; margin: 5px 5px; padding-right: 20px;">
	                	<a href="<%=Fritz.www_path+"?id="+file.fileName() %>" target="_blank"  >
	                		<img src="bilder/play.png" id="<%=file.fileName() %>"  />
	                	</a>
	                	<p><%=id3[0]+" - "+id3[1] %></p>
	                	<form method="POST" style="display: inline; ">
	                		<input type="hidden" name="delete" value="<%=file.fileName() %>" />
	                		<input type="image" src="bilder/x.png" style="position: absolute; top: 0px; right: 2px;" />
	                	</form>
                	</div>
                </td>
       <% i++;
                 if((i%3) == 0)
                	 out.print("</tr><tr>");
            } out.print("</tr>");
       %>
       </table>	
    </div>
	<% out.println("test: "+request.getContextPath());%>
</body>
</html>