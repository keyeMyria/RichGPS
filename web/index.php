<?php
  require 'vendor/autoload.php';
  $app = new \Slim\Slim();
  $app->add(new \Slim\Middleware\ContentTypes());
  $db = mysql_connect('localhost', 'gps', 'oafgk4ac');
  if (!$db) {
    echo "Database connection fail.";
  }
  mysql_query("USE locations;", $db);

  $vehicle_keys = array("1" => "key1", "2" => "key2", "3" => "key3");

  //Get all previous locations for a vehicle (more recent than some time)
  $app->get('/history/:id/(:since)', function($id, $since = 0) use ($app, $db) {
    $app->response()->header("Content-Type", "application/json");
    $result = mysql_query('SELECT time, lat, lng, speed FROM full_reports
      WHERE vehicle_id = ' .$id . ' AND time > ' . $since . '
      ORDER BY time;');
    if (!$result) {
      echo '{}';
    } else {
      $all_data = array();
      while ($row = mysql_fetch_array($result)) {
        $time = $row["time"];
        $lat = $row["lat"];
        $long = $row["lng"];
        $speed = $row["speed"];
        $all_data[] = array("time" => $time, "lat" => $lat, "lng" => $long, "speed" => $speed);
      }
      echo json_encode($all_data);
    }
  });

  //Get most recent location of one (or all) vehicles.
  $app->get('/current/(:id)', function($id = -1) use ($app, $db) {
    $app->response()->header("Content-Type", "application/json");
    if ($id == -1) {
      $result = mysql_query('SELECT loc.vehicle_id, loc.time, loc.lat, loc.lng, loc.speed FROM full_reports loc 
        INNER JOIN (
          SELECT vehicle_id, max(time) time 
          FROM full_reports
          GROUP BY vehicle_id
        ) max_time ON loc.vehicle_id = max_time.vehicle_id AND loc.time = max_time.time;');
    } else {
      $result = mysql_query('SELECT loc.vehicle_id, loc.time, loc.lat, loc.lng, loc.speed FROM full_reports loc 
        INNER JOIN (
          SELECT max(time) time
          FROM full_reports
          WHERE vehicle_id = ' . $id . '
        ) max_time ON loc.vehicle_id = ' . $id . ' AND loc.time = max_time.time;');
    }
    if (!$result) {
      echo '{}';
    } else {
      $all_data = array();
      while ($row = mysql_fetch_array($result)) {
        $veh = $row["vehicle_id"];
        $time = $row["time"];
        $lat = $row["lat"];
        $long = $row["lng"];
        $speed = $row["speed"];
        $report = array("id" => $veh, "time" => $time, "lat" => $lat, "lng" => $long, "speed" => $speed);
        if ($id != -1) {
          echo json_encode($report);
          return;
        } else {
          $all_data[] = $report;
        }
      }
      echo json_encode($all_data);
    }
  });

  //Add new location data.
  $app->post('/add/:key', function($key) use ($app, $db, $vehicle_keys) {
    $req = $app->request();
    $params = $req->getBody();
    $id = $params{'id'};
    $rep_time = $params{'report_time'};
    $time = $params{'time'};
    $status = $params{'status'};
    $lat = $status{'lat'};
    $lng = $status{'lng'};
    $speed = $status{'speed'};
    if ($vehicle_keys[$id] != $key) {
      $app->response()->status(400);
    } else {
      mysql_query("INSERT INTO status VALUES (DEFAULT, $lat, $lng, $speed);");
      mysql_query("INSERT INTO reports VALUES (DEFAULT, $id, $rep_time, $time, LAST_INSERT_ID());");
    }
  });

  $app->delete('/reset', function() use ($app, $db) {
    mysql_query("DELETE FROM status;");
    mysql_query("DELETE FROM reports;");
    mysql_query("DELETE FROM schedules;");
    mysql_query("DELETE FROM waypoints;");
    mysql_query("DELETE FROM arrivals;");
  });

  $app->get('/schedule/:vehicle', function($vehicle) use ($app, $db) {
    $result = mysql_query("SELECT waypoint, due_time, name FROM schedules, waypoints WHERE waypoint = wp_id AND vehicle = $vehicle
      AND (SELECT Count(*) FROM arrivals WHERE vehicle = $vehicle AND waypoint = schedules.waypoint) = 0;");
    if (!$result) {
      echo "{}";
    } else {
      $all_data = array();
      while ($row = mysql_fetch_array($result)) {
        $wp = $row["waypoint"];
        $due = $row["due_time"];
        $name = $row["name"];
        $report = array("waypoint" => $wp, "due_time" => $due, "waypoint_name" => $name);
        $all_data[] = $report;
      }
      echo json_encode($all_data);
    }
  });

  $app->get('/waypoints', function() use ($app, $db) {
    $result = mysql_query("SELECT wp_id, name FROM waypoints;");
    if (!$result) {
      echo "{}";
    } else {
      $all_data = array();
      while ($row = mysql_fetch_array($result)) {
        $id = $row["wp_id"];
        $name = $row["name"];
        $report = array("id" => $id, "name" => $name);
        $all_data[] = $report;
      }
      echo json_encode($all_data);
    }
  });

  $app->post('/add_schedule', function() use ($app, $db) {
    $req = $app->request();
    $params = $req->getBody();
    $id = $params{'id'};
    $waypoint = $params{'waypoint'};
    $time = $params{'due_time'};
    mysql_query("INSERT INTO schedules VALUES (DEFAULT, $id, $waypoint, $time);");
  });

  $app->post('/add_waypoint', function() use ($app, $db) {
    $req = $app->request();
    $params = $req->getBody();
    $name = $params{'waypoint_name'};
    mysql_query("INSERT INTO waypoints VALUES (DEFAULT, \"$name\");");
  });

  $app->post('/visit_waypoint/:key', function($key) use ($app, $db, $vehicle_keys) {
    $req = $app->request();
    $params = $req->getBody();
    $id = $params{'id'};
    $waypoint = $params{'waypoint'};
    $arrival_time = $params{'arrival_time'};
    if ($vehicle_keys[$id] != $key) {
      $app->response()->status(400);
    } else {
      mysql_query("INSERT INTO arrivals VALUES (DEFAULT, $id, $waypoint, $arrival_time);");
    }
  });

  $app->run();
  mysql_close($db);
?>
