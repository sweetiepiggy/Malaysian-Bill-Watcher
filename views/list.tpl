<html>
<head>
    <title>Malaysian Bill Watcher</title>
    <link rel="alternate" type="application/rss+xml" title="RSS feed" href="/feeds/" />
    <link rel="stylesheet" type="text/css" media="all" href="/css/reset.css" />
    <link rel="stylesheet" type="text/css" media="all" href="/css/text.css" />
    <link rel="stylesheet" type="text/css" media="all" href="/css/960.css" />
    <link rel="stylesheet" type="text/css" media="all" href="/css/bills.css" />
</head>
<body>
<div id='header'>
</div>
<body>
    <div class="container_12">
        <div class="grid_12 header">
            <h1>Malaysian Bill Watcher</h1>
        </div>
        <div class="clear"></div>
        <div class="grid_3">
            <p>This project is created to create an awareness of law enacted, by your MP's</p>
        </div>
        <div class="grid_9">
            %for i in bill:
                <div class="bill_item">
                <h5><a href="/detail/{{i.bill_id}}/">{{i.long_name}}</a></h5>
                Revision: {{i.year}}
                <br>Status  : {{i.status}}</br>
                </div>
            %end
            <div class='pages' id='pages'>
            %if page_no != page_list[0]:
                <a href='/?page_no={{prev_page}}'>prev</a>
            %end
            %for i in page_list:
                %if i == page_no:
                    {{i}}
                %else:
                    <a href='/?page_no={{i}}'>{{i}}</a>
                %end
            %end
            %if page_no != page_list[-1]:
                <a href='/?page_no={{next_page}}'>next</a>
            %end
            </div>
        </div>
    </div>
</body>
</body>
</html>