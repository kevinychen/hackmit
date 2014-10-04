var express = require('express');
var http = require('http');
var request = require('request');
var exec = require('child_process').exec;

var port = 8101;
var app = express();
app.use(express.bodyParser());
app.use(express.logger('dev'));

/*
 * {location: '42.35904,-71.095174'}
 *  -> {summary: 'MIT is a university...'}
 */
app.get('/getPOIs', function(req, res) {
    var location = req.query.location;
    var radius = req.query.radius || 100;
    request.get({
        url: 'https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=AIzaSyDjPSTLIvxabT87GCJM9nHyi0QaYwvBEtQ&location='
                + location + '&radius=' + radius
    }, function(err, data) {
        if (err || !data || !data.body) {
            console.log('Fail');
            return;
        }
        var results = JSON.parse(data.body).results;
        console.log('Found ' + results.length + ' results.');
        var name = results[0].name.replace(/\W/g, ' ');
        exec('python places.py "' + name + '" 3', function(err, stdout, stderr) {
            if (err || stderr) {
                res.json({err: err || stderr});
            } else {
                res.json({summary: stdout});
            }
        });
    });
});

http.createServer(app).listen(port);
