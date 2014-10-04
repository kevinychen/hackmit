var fb = new Firebase("https://waypal.firebaseio.com");
var directionsDisplay;
var directionsService = new google.maps.DirectionsService();
var map;

function initialize() {
  directionsDisplay = new google.maps.DirectionsRenderer();
  var chicago = new google.maps.LatLng(41.850033, -87.6500523);
  var mapOptions = {
    zoom:7,
    center: chicago
  };
  map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
  directionsDisplay.setMap(map);
}

// callback(err, data);
function getWaypoints(tripId, callback) {
  fb.child(tripId).once('value', function(data) {
    var waypoints = data.val();
    if (!waypoints) {
      callback("Error obtaining waypoints! Does this trip exist?", "");
      return;
    }
    callback(false, waypoints);
  });
}

getWaypoints('trip1', function(err, data) {
  console.log(data);
});

function calcRoute() {
  var start = document.getElementById('start').value;
  var end = document.getElementById('end').value;
  var request = {
      origin:start,
      destination:end,
      travelMode: google.maps.TravelMode.DRIVING
  };
  directionsService.route(request, function(response, status) {
    if (status == google.maps.DirectionsStatus.OK) {
      directionsDisplay.setDirections(response);
    }
  });
}

google.maps.event.addDomListener(window, 'load', initialize);
