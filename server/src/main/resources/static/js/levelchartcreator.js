

function setDateTimeToAndFromLastDay() {
    var dateFrom = new Date();
    dateFrom.setDate(dateFrom.getDate()-1);
    $("#datetimefrom")[0].valueAsNumber = dateFrom.getTime();
    $('#datetimeto')[0].valueAsNumber = new Date().getTime();
}

function setTimeBoundsAndStartBackgroundUpdater() {
    setDateTimeToAndFromLastDay();
    setInterval(function() {
      onBtnLoadChart();
    }, 60000);
}

function loadChart(paramType, paramId, datetimefrom, datetimeto) {
    $.get( "api/v1/indications?paramType=" + paramType + "&paramId=" + paramId + "&datetimefrom=" + datetimefrom + "&datetimeto=" + datetimeto, function( data ) {
        var parsedDatetimeFrom = new Date(datetimefrom);
        var parsedDatetimeTo = new Date(datetimeto);
        let chart = {
                      chart: {
                        type: 'spline'
                      },
                      title: {
                        text: data.name
                      },
                      subtitle: {
                        text: data.title
                      },
                      xAxis: {
                        type: 'datetime',
                        dateTimeLabelFormats: { // don't display the dummy year
                          month: '%e. %b',
                          year: '%b'
                        },
                        title: {
                          text: 'Date'
                        },
                        min: parsedDatetimeFrom.valueOf(),
                        max: parsedDatetimeTo.valueOf()
                      },
                      yAxis: {
                        title: {
                          //text: 'Snow depth (m)'
                        },
                        min: 0
                      },
                      tooltip: {
                        headerFormat: '<b>{series.name}</b><br>',
                        pointFormat: '{point.x:%e. %b}:<br>{point.y:.2f}'
                      },

                      plotOptions: {
                        series: {
                          marker: {
                            enabled: true
                          }
                        }
                      },

                      colors: ['#6CF', '#39F', '#06C', '#036', '#000'],

                      // Define the data points. All series have a dummy year
                      // of 1970/71 in order to be compared on the same x axis. Note
                      // that in JavaScript, months start at 0 for January, 1 for February etc.
                      series: data.series,

                      responsive: {
                        rules: [{
                          condition: {
                            maxWidth: 500
                          },
                          chartOptions: {
                            plotOptions: {
                              series: {
                                marker: {
                                  radius: 2.5
                                }
                              }
                            }
                          }
                        }]
                      }
                    };
        Highcharts.chart('container', chart);
    });

}