var express = require('express');
var http = require('http');
var request = require('request');
var exec = require('child_process').exec;

var port = 8101;
var maxPOIs = 3;
var numSentences = 3;
var app = express();
app.use(express.bodyParser());
app.use(express.logger('dev'));

/*
 * {location: '42.35904,-71.095174'}
 *  -> {error: false, POIs: [name: 'MIT', location: '42.35904,-71.095174', summary: 'MIT is a university...']}
 */
app.get('/getPOIs', function(req, res) {
    var location = req.query.location;
    var radius = req.query.radius || 100;
    request.get({
        url: 'https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=AIzaSyDjPSTLIvxabT87GCJM9nHyi0QaYwvBEtQ&location='
                + location + '&radius=' + radius
    }, function(err, data) {
        if (err || !data || !data.body) {
            return console.log('Fail');
        }
        var results = JSON.parse(data.body).results;
        results = results.slice(0, maxPOIs);
        console.log('Found ' + results.length + ' results.');
        if (results.length === 0) {
            return res.json({error: 'No results'});
        }

        var POIs = new Array(results.length);
        var counter = results.length;
        var error = false;
        var getSummary = function(name, location, i) {
            exec('python places.py "' + name + '" ' + numSentences, function(err, stdout, stderr) {
                if (err || stderr) {
                    error |= err || stderr;
                } else {
                    POIs[i] = {name: name, location: location, summary: stdout};
                }
                if (--counter == 0) {
                    return res.json({error: error, POIs: POIs});
                }
            });
        };

        for (var i = 0; i < maxPOIs; i++) {
            var name = results[i].name.replace(/\W/g, ' ');
            var location = results[i].geometry.location;
            getSummary(name, location, i);
        }
    });
});

http.createServer(app).listen(port);
