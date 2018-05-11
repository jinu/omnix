<#import "/common/lib.ftl" as lib> 
<@lib.header />

    <!-- begin #page-container -->
    <div id="page-container" class="fade">
        <div class="login-cover">
            <div class="login-cover-image" style="background-image: url(/files/img/login-bg/login-bg-13.jpg)" data-id="login-cover-image"></div>
            <div class="login-cover-bg"></div>
        </div>
    
        <!-- begin login -->
        <div class="login login-v2" data-pageload-addclass="animated fadeIn">
            <!-- begin brand -->
            <div class="login-header">
                <div class="brand">
                    <span class="logo"></span> <b>Omnix</b> Search
                    <small>Simple and Fastest search engine demo</small>
                </div>
                <div class="icon">
                    <i class="fa fa-lock"></i>
                </div>
            </div>
            <!-- end brand -->
            <!-- begin login-content -->
            <div class="login-content">
                <form action="/sample" method="GET" class="margin-bottom-0">
                    <div class="form-group m-b-20">
                        <input type="text" class="form-control form-control-lg" placeholder="Email Address" required />
                    </div>
                    <div class="form-group m-b-20">
                        <input type="password" class="form-control form-control-lg" placeholder="Password" required />
                    </div>
                    <div class="checkbox checkbox-css m-b-20">
                        <input type="checkbox" id="remember_checkbox" /> 
                        <label for="remember_checkbox">
                            Remember Me
                        </label>
                    </div>
                    <div class="login-buttons">
                        <button type="submit" class="btn btn-success btn-block btn-lg">Sign me in</button>
                    </div>
                    <div class="m-t-20">
                        Not a member yet? Click <a href="javascript:;">here</a> to register.
                    </div>
                </form>
            </div>
            <!-- end login-content -->
        </div>
        <!-- end login -->
        
        <ul class="login-bg-list clearfix">
            <li class="active"><a href="javascript:;" data-click="change-bg" data-img="/files/img/login-bg/login-bg-13.jpg" style="background-image: url(/files/img/login-bg/login-bg-13.jpg)"></a></li>
            <li><a href="javascript:;" data-click="change-bg" data-img="/files/img/login-bg/login-bg-10.jpg" style="background-image: url(/files/img/login-bg/login-bg-10.jpg)"></a></li>
            <li><a href="javascript:;" data-click="change-bg" data-img="/files/img/login-bg/login-bg-3.jpg" style="background-image: url(/files/img/login-bg/login-bg-3.jpg)"></a></li>
            <li><a href="javascript:;" data-click="change-bg" data-img="/files/img/login-bg/login-bg-12.jpg" style="background-image: url(/files/img/login-bg/login-bg-12.jpg)"></a></li>
        </ul>
        
    </div>
    <!-- end page container -->
    
    <!-- ================== BEGIN BASE JS ================== -->
    <script src="/files/plugins/jquery/jquery-3.2.1.min.js"></script>
    <script src="/files/plugins/jquery-ui/jquery-ui.min.js"></script>
    <script src="/files/plugins/bootstrap/4.0.0/js/bootstrap.bundle.min.js"></script>
    <!--[if lt IE 9]>
        <script src="/files/crossbrowserjs/html5shiv.js"></script>
        <script src="/files/crossbrowserjs/respond.min.js"></script>
        <script src="/files/crossbrowserjs/excanvas.min.js"></script>
    <![endif]-->
    <script src="/files/plugins/slimscroll/jquery.slimscroll.min.js"></script>
    <script src="/files/plugins/js-cookie/js.cookie.js"></script>
    <script src="/files/js/theme/default.min.js"></script>
    <script src="/files/js/apps.min.js"></script>
    <!-- ================== END BASE JS ================== -->
    
    <!-- ================== BEGIN PAGE LEVEL JS ================== -->
    <script src="/files/js/demo/login-v2.demo.min.js"></script>
    <!-- ================== END PAGE LEVEL JS ================== -->

    <script>
        $(document).ready(function() {
            App.init();
            LoginV2.init();
        });
    </script>
</body>
</html>
