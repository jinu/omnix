</div>
<!-- end page container -->
    <script>
    	var socket = new SockJS('/stomp');
		var stompClient = Stomp.over(socket);
		var stompHeader = {};
		
		stompClient.connect(stompHeader, function(frame) {
			if (typeof(stompClientHook) == 'function') {
				stompClientHook(stompClient, frame);
			}
		});
    
        $(document).ready(function() {
            App.init();
        });
    </script>
</body>
</html>