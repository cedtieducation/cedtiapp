<?php
require_once __DIR__.'/../db.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_out(['ok'=>false,'error'=>'POST required'],405);
$staff=require_staff();
$in=json_decode(file_get_contents('php://input'),true) ?: [];
$type=strtoupper(trim((string)($in['type']??'')));
if (!in_array($type,['IN','OUT'],true)) json_out(['ok'=>false,'error'=>'Type must be IN or OUT'],422);
$lat=filter_var($in['latitude']??null,FILTER_VALIDATE_FLOAT); $lng=filter_var($in['longitude']??null,FILTER_VALIDATE_FLOAT);
$accuracy=filter_var($in['accuracy']??null,FILTER_VALIDATE_FLOAT);
if ($lat===false || $lng===false || $lat < -90 || $lat > 90 || $lng < -180 || $lng > 180) json_out(['ok'=>false,'error'=>'Valid GPS coordinates are required'],422);
if ($accuracy===false || $accuracy < 0 || $accuracy > 100) json_out(['ok'=>false,'error'=>'GPS accuracy is too weak. Please wait for a better GPS signal.'],422);
// Branch pin coordinates supplied by CEDTI. Radius is enforced on the server.
$branches = require __DIR__.'/../branch_locations.php';
$branch=(string)$staff['branch'];
if (!isset($branches[$branch])) json_out(['ok'=>false,'error'=>'Branch GPS location is not configured. Contact administrator.'],409);
$target=$branches[$branch];
$earth=6371000.0; $p1=deg2rad((float)$target['lat']); $p2=deg2rad((float)$lat);
$dp=deg2rad((float)$lat-(float)$target['lat']); $dl=deg2rad((float)$lng-(float)$target['lng']);
$a=sin($dp/2)**2 + cos($p1)*cos($p2)*sin($dl/2)**2;
$distance=$earth*2*atan2(sqrt($a),sqrt(max(0,1-$a)));
$radius=(float)($target['radius_m']??20);
if ($distance > $radius) json_out(['ok'=>false,'error'=>'Attendance allowed only within '.$radius.' metres of your '.$branch.' centre. Current distance: '.round($distance,1).' m.'],403);
$q=db()->prepare('INSERT INTO attendance (staff_id,branch,attendance_type,server_time,latitude,longitude) VALUES (?,?,?,NOW(),?,?)');
$q->execute([$staff['id'],$branch,$type,$lat,$lng]);
$serverTime=db()->query('SELECT DATE_FORMAT(NOW(), "%Y-%m-%d %H:%i:%s")')->fetchColumn();
json_out(['ok'=>true,'message'=>$type==='IN'?'Check-in recorded successfully':'Check-out recorded successfully','server_time'=>$serverTime,'distance_m'=>round($distance,1),'radius_m'=>$radius]);
