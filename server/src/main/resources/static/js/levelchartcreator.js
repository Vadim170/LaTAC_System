Date.prototype.setDefaultDate = (function() {
    return this.getFullYear() + "-" + ("0"+(this.getMonth()+1)).slice(-2) + "-" + ("0" + this.getDate()).slice(-2) + "T" + ("0" + this.getHours()).slice(-2) + ":" + ("0" + this.getMinutes()).slice(-2);
    // "2020-05-14T14:54:33.775" =
});

function setDateTimeToAndFromLastDay() {
    var dateFrom = new Date();
    dateFrom.setDate(dateFrom.getDate()-1);
    try {
        $("#datetimefrom").val(dateFrom.setDefaultDate());
        $("#datetimeto").val(new Date().setDefaultDate());
    } catch(e) {
        console.log('Ошибка ' + e.name + ":" + e.message + "\n" + e.stack); // (3) <--
    }
    try {
        $("#datetimefrom")[0].valueAsNumber = dateFrom.getTime();
        $('#datetimeto')[0].valueAsNumber = new Date().getTime();
    } catch(e) {
        console.log('Ошибка ' + e.name + ":" + e.message + "\n" + e.stack); // (3) <--
    }
}

function setTimeBoundsAndStartBackgroundUpdater() {
    setDateTimeToAndFromLastDay();
    onBtnLoadChart();
    setInterval(function() {
        onBtnLoadChart();
    }, 60000);
}

function chartAndDiagramUpdate(paramType, paramId, datetimefrom, datetimeto) {
    loadChart(paramType, paramId, datetimefrom, datetimeto);
    loadDiagram(paramType, paramId);
}

function loadChart(paramType, paramId, datetimefrom, datetimeto) {
	$.get("api/v1/indications?paramType=" + paramType + "&paramId=" + paramId + "&datetimefrom=" + datetimefrom + "&datetimeto=" + datetimeto, function(data) {
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
                pointFormat: '{point.x:%e.%b/%H:%M }:<br>{point.y:.2f}'
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

function loadDiagram(paramType, paramId) {
	$.get("api/v1/indicationsLast?paramType=" + paramType + "&paramId=" + paramId, function(data) {
	    if(paramType !== "Temperature") return;
	    let reversedDataSeries = data.series
        reversedDataSeries[0].data = reversedDataSeries[0].data.reverse();
        let size = reversedDataSeries[0].data.length
        let lables = Array.from({ length: size }, (v, i) =>  size - i);
		let chart = {
			chart: {
				type: 'bar'
			},
			title: {
				text: data.name
			},
			subtitle: {
				text: data.title
			},
			xAxis: {
				min: 0,
                categories: lables,
				title: {
					text: null
				}
			},
			yAxis: {
				min: 0,
				title: {
					text: 'Temperature (°C)',
					align: 'high'
				},
				labels: {
					overflow: 'justify'
				}
			},
			plotOptions: {
				bar: {
					dataLabels: {
						enabled: true
					}
				}
			},
			legend: {
                enabled: false,
				layout: 'vertical',
				align: 'right',
				verticalAlign: 'top',
				x: -40,
				y: 80,
				floating: true,
				borderWidth: 1,
				backgroundColor: Highcharts.defaultOptions.legend.backgroundColor || '#FFFFFF',
				shadow: true
			},
			credits: {
				enabled: false
			},
			series: reversedDataSeries
		};
		Highcharts.chart('containerDiagram', chart);
	});
}