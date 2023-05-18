function refreshData(doRefresh){
        xmlHttp = false;
        if (window.XMLHttpRequest) { // Mozilla, Safari,...
            xmlHttp = new XMLHttpRequest();
            if (xmlHttp.overrideMimeType) {
                xmlHttp.overrideMimeType('text/html');
            }
        } else if (window.ActiveXObject) { // IE
            try {
                xmlHttp = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
                } catch (e) {}
            }
        }
        if (!xmlHttp) {
            alert('Cannot create XMLHTTP instance');
            return false;
        }
        try{
            var url = location.origin+"/json";
            if(doRefresh === 1){
                url = url+"?ref=1";
            }
            xmlHttp.onreadystatechange = ajaxCallHandler.bind({doRefresh});
            xmlHttp.open("GET", url, true);
            xmlHttp.setRequestHeader("Accept", "application/json");
            xmlHttp.send(null);
        }catch(e){
               alert("Unable to process the request at this moment");
               return false;
        }
    }
function ajaxCallHandler() {
    if (xmlHttp.readyState != 4) {
        if(this.doRefresh) document.getElementById('loader').style.display='block' ;
    }
    if (xmlHttp.readyState == 4) {
        if (xmlHttp.status == 200) {
            bindDataInHtml(xmlHttp.response);
            document.getElementById('loader').style.display='none' ;
        } else {
            alert("There was a problem with the request.");
        }
    }
}

function bindDataInHtml(data){
    try{
        let parsedData = JSON.parse(data);

        if(parsedData.overall && parsedData.overall === "ok") document.getElementById('overall').innerHTML = "<i class='fa fa-check-circle' style='color:green'></i>" ;
        else document.getElementById('overall').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;

        if(parsedData.database && parsedData.database === "ok") document.getElementById('database').innerHTML = "<i class='fa fa-check-circle' style='color:green'></i> "+(parsedData.db_storage.db_storage_usage)+"% of "+(parsedData.db_storage.db_storage_total || '--') ;
        else document.getElementById('database').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;
        if(parsedData.db_storage.bk_storage_usage && parsedData.db_storage.bk_storage_total) document.getElementById('database').innerHTML += "<div style='font-size:12px'>Backup: "+parsedData.db_storage.bk_storage_usage+" of "+ parsedData.db_storage.bk_storage_total+"</div>"

        if(parsedData.nas) document.getElementById('nas').innerHTML = "<span>"+(parsedData.nas_usage >= 95 ? "<i class='fa fa-close' style='color:red'></i>" : "<i class='fa fa-check-circle' style='color:green'></i>")+" "+parsedData.nas+"</span>" ;
        else document.getElementById('nas').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;

        if(parsedData.other && parsedData.other !== "not ok") document.getElementById('other').innerHTML = "<span>"+parsedData.other+"</span>" ;
        else document.getElementById('other').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;

        if(parsedData.running_pods != null) document.getElementById('running_pods').innerHTML = "<span>"+parsedData.running_pods+"</span>" ;
        else document.getElementById('running_pods').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;

        if(parsedData.orchies != null) document.getElementById('orchies').innerHTML = "<span>"+parsedData.orchies+"</span>" ;
        else document.getElementById('orchies').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;

        if(parsedData.last_refreshed){
            if(parsedData.last_refreshed.hours > 0) document.getElementById('last_refreshed').innerHTML = "<span>"+parsedData.last_refreshed.hours+" Hours Ago</span>" ;
            if(parsedData.last_refreshed.hours === 0 && parsedData.last_refreshed.minutes > 0) document.getElementById('last_refreshed').innerHTML = "<span>"+parsedData.last_refreshed.minutes+" Minutes Ago</span>" ;
            if(parsedData.last_refreshed.hours === 0 && parsedData.last_refreshed.minutes === 0) document.getElementById('last_refreshed').innerHTML = "<span>"+parsedData.last_refreshed.seconds+" Seconds Ago</span>" ;
        } else document.getElementById('last_refreshed').innerHTML = "<i class='fa fa-close' style='color:red'></i>" ;
        if(parsedData.error_msg && parsedData.error_msg.length){
            let _err = "";
            for(let i = 0; i < parsedData.error_msg.length; i++){
                _err += "<p>"+(i+1)+". "+parsedData.error_msg[i]+"</p>"
            };
            document.getElementById('errors').innerHTML = _err ;
        } else document.getElementById('errors').innerHTML = "<span>N/A</span>" ;
        let cd = parsedData.clientDetails || [];
        if(cd.length === 0){
            document.getElementById("connection_grid").innerHTML = "<span>No Data</span>";
            return;
        }
        let orchyList = cd[0];
        let compyList = cd[1];
        let conn = cd[2];
        let table = "<table class='table conn_table table-bordered'>";
        table += "<tr> <th class='compy_col'></th>";
        for(let i =0; i < orchyList.length; i++){
            table += "<th style='font-weight:bold; font-size:13px;text-align:center'><span style='cursor:pointer; color:blue' onclick='openK8Dashboard(\""+parsedData.k8url+"\",\""+orchyList[i][0]+"\")'>"+orchyList[i][0]+"</span><p>(<span>"+orchyList[i][2]+"</span>/<span>"+orchyList[i][1]+"</span>)</p></th>";
        }
        table += "</tr>";
        for(let i = 0; i < compyList.length; i++){
            table += "<tr><td class='compy_col'><span class='podname' onclick='openK8Dashboard(\""+parsedData.k8url+"\",\""+compyList[i]+"\")'>"+compyList[i]+"</span></td>";
            for(let j = 0; j < conn[compyList[i]].length; j++){
                table += "<td style='text-align:center'>";
                if(conn[compyList[i]][j]){
                    if(conn[compyList[i]][j][0] === 'ACTIVE' && !conn[compyList[i]][j][1]) table += " <i class='fa fa-check-circle' style='color:green'></i>";
                    else if(conn[compyList[i]][j][0] === 'ACTIVE' && conn[compyList[i]][j][1]) table += " <i class='fa fa-check-circle' style='color:orange'></i>";
                    if(conn[compyList[i]][j][0] === 'INACTIVE') table += " <i class='fa fa-close' style='color:red'></i>";
                }else{
                    table += "<span>N/A</span>";
                }
                table += "</td>"
            }
            table += "</tr>"
        }
        table += "</table>";
        document.getElementById("connection_grid").innerHTML = table;
    }catch(ex){
        console.log("Error while parsing response", ex);
        return;
    }
}

function openK8Dashboard(k8url, podName){
    if(podName && k8url){
        window.open(k8url.replace("::pod_name::", podName), "_blank");
    }
}
refreshData(0);
var interval = null;
interval = setInterval(function(){
    refreshData(0);
},5000);