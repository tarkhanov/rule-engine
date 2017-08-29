
function loadMonitoringCharts(webSocketURL) {

  var cpuChart = new Highcharts.Chart({
    chart: {
      renderTo: 'cpuChart',
      defaultSeriesType: 'spline',
      backgroundColor: "#FFFFFF",
      spacing: [20,0,0,0],
      height: 300,
      animation: { duration: 900 }
    },
    legend: { enabled: false },
    title: { text: "" },
    yAxis: {
      gridLineColor: "#E0E0E0",
      labels: { format: "{value}%" },
      max: 100,
      min: 0,
      title: { text: "" }
    },
    xAxis: { type: 'datetime' },
    series: [{
      name: "System",
      data: [],
      color: "#DC7614",
      enableMouseTracking: false,
      marker: { enabled : false }
    },{
      name: "Process",
      data: [],
      color: "#DC143C",
      enableMouseTracking: false,
      marker: { enabled : false }
    }],
    credits: { enabled: false }
  });

  var memoryChart = new Highcharts.Chart({
    chart: {
      renderTo: 'memoryChart',
      defaultSeriesType: 'spline',
      backgroundColor: "#FFFFFF",
      spacing: [20,0,0,0],
      height: 300,
      animation: { duration: 900 }
    },
    legend: { enabled: false },
    title: { text: "" },
    yAxis: {
      gridLineColor: "#E0E0E0",
      labels: { format: "{value}MB" },
      min: 0,
      title: { text: "" }
    },
    xAxis: { type: 'datetime' },
    series: [{
      name: "Used",
      data: [],
      color: "#0DA76F",
      enableMouseTracking: false,
      marker: { enabled : false }
    }, {
      name: "Total",
      data: [],
      color: "#0FD854",
      enableMouseTracking: false,
      marker: { enabled : false }
    }],
    credits: { enabled: false }
  });

  var socket = new WebSocket(webSocketURL)
  socket.onmessage = function(event) {
    var datapoint = jQuery.parseJSON(event.data);
    cpuChart.series[0].addPoint({ x: datapoint.ts, y: parseInt(datapoint.sl) }, false, cpuChart.series[0].data.length >= 60 );
    cpuChart.series[1].addPoint({ x: datapoint.ts, y: parseInt(datapoint.pl) }, false, cpuChart.series[1].data.length >= 60 );
    cpuChart.redraw();
    memoryChart.series[0].addPoint({ x: datapoint.ts, y: parseInt(datapoint.um) }, false, memoryChart.series[0].data.length >= 60 );
    memoryChart.series[1].addPoint({ x: datapoint.ts, y: parseInt(datapoint.tm) }, false, memoryChart.series[1].data.length >= 60 );
    memoryChart.redraw();
  }

}