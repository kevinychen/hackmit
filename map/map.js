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

fb.child('trip1').on('value', function(data) {
  var waypoints = data.val();
  if (!waypoints) {
    callback("Error obtaining waypoints! Does this trip exist?", "");
    return;
  }
  // Ideally you would call calcRoute(waypoints);
  calcRoute();
});

function addWaypointToTrip(tripId, waypointNumber, waypointObj) {
  fb.child(tripId).child('waypoints').once('value', function(data) {
    var waypoints = data.val();
    if (waypoints) {
      waypoints['waypoint' + waypointNumber] = waypointObj;
      fb.child(tripId).child('waypoints').set(waypoints);
    }
  });
}

function appendWaypoint(tripId, waypointObj) {
  fb.child(tripId).child('waypoints').once('value', function(data) {
    var waypoints = data.val();
    if (waypoints) {
      var num = Object.keys(waypoints).length + 1;
      addWaypointToTrip(tripId, num, waypointObj);
    }
  });
}

function addTrip(tripId, waypointsList) {
  fb.child(tripId).child('waypoints').set(waypointsList);
}

addTrip('trip2', {'waypoint1': {'location':{'lat': 24.222, 'lng': 27.211}, 'name': 'supdawg', 'stopover':true}});
appendWaypoint('trip2', {'location':{'lat': 17, 'long':42}, 'name':'yo', 'stopover':true});


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

$(document).ready(function() {
  $("#start, #end").keyup(function(event) {
    if (event.keyCode == 13) {
      calcRoute();
    }
  });
});
