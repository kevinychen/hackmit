var fb = new Firebase("https://waypal.firebaseio.com");
var directionsDisplay;
var directionsService = new google.maps.DirectionsService();
var map;
var waypoints;

function initialize() {
  directionsDisplay = new google.maps.DirectionsRenderer();
  var mapOptions = {
    zoom: 15,
  };
  map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
  directionsDisplay.setMap(map);

  var mit = new google.maps.LatLng(42.360190, -71.094165);
  if(navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
      var pos = new google.maps.LatLng(position.coords.latitude,
        position.coords.longitude);
      map.setCenter(pos);
    }, function() {
      map.setCenter(mit);
    });
  } else {
    // Browser doesn't support Geolocation
    map.setCenter(mit);
  }
}

fb.child('trip1').child('waypoints').on('value', function(data) {
  var waypointsObj = data.val();
  if (!waypointsObj) {
    console.log("Error obtaining waypoints! Does this trip exist?");
    return;
  }
  // Convert to array
  waypoints = [];
  for (var key in waypointsObj) {
    var waypoint = waypointsObj[key];
    var latlng = new google.maps.LatLng(waypoint.location.lat,
      waypoint.location.lng);
    waypoints.push({
      location: latlng.toString(),
      stopover: true
    });
  }
  console.log(waypoints);
  calcRoute();
});

function calcRoute() {
  var start = document.getElementById('start').value;
  var end = document.getElementById('end').value;
  var request = {
    origin: start,
    destination: end,
    travelMode: google.maps.TravelMode.DRIVING,
    waypoints: waypoints
  };
  directionsService.route(request, function(response, status) {
    if (status == google.maps.DirectionsStatus.OK) {
      directionsDisplay.setDirections(response);
      var route = response.routes[0];
      console.log(route);
    }
  });
}

google.maps.event.addDomListener(window, 'load', initialize);

$(document).ready(function() {
  $("#start, #end").keyup(function(event) {
    if (event.keyCode == 13) {
      calcRoute();
    }
  });
});
