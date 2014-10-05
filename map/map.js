var fb = new Firebase("https://waypal.firebaseio.com");
var directionsDisplay;
var directionsService = new google.maps.DirectionsService();
var map;
var geocoder;
var start = "", end = "", waypoints;

function initialize() {
  var mapOptions = {
    zoom: 15,
  };
  map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
  directionsDisplay = new google.maps.DirectionsRenderer();
  directionsDisplay.setMap(map);
  geocoder = new google.maps.Geocoder();

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

// Listen for Firebase changes
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
    var latlng = new google.maps.LatLng(waypoint.lat,
      waypoint.lng);
    waypoints.push({
      location: latlng.toUrlValue(),
      stopover: true
    });
  }
  console.log(waypoints);
  calcRoute();
});

function calcRoute() {
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
    } else {
      console.log(response);
    }
  });
}

// callback(array, status)
function geocode(address, callback) {
  var request = {
    address: address,
    bounds: map.getBounds()
  };
  geocoder.geocode(request, callback);
}

// Get LatLng from geocode response
function getLatLng(response) {
  return response[0].geometry.location;
}

google.maps.event.addDomListener(window, 'load', initialize);

$(document).ready(function() {
  $("#start, #end").keyup(function(event) {
    if (event.keyCode == 13) {
      geocode($("#start").val(), function(resp1, status1) {
        if (status1 == google.maps.GeocoderStatus.OK) {
          start = getLatLng(resp1);
          geocode($("#end").val(), function(resp2, status2) {
            if (status2 == google.maps.GeocoderStatus.OK) {
              end = getLatLng(resp2);
              calcRoute();
            }
          });
        }
      });
    }
  });
});
