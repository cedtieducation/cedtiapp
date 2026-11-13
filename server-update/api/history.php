<?php
require_once __DIR__.'/../db.php';
if ($_SERVER['REQUEST_METHOD'] !== 'GET') json_out(['ok'=>false,'error'=>'GET required'],405);
$staff=require_staff();
$q=db()->prepare('SELECT attendance_type,branch,server_time,latitude,longitude FROM attendance WHERE staff_id=? ORDER BY server_time DESC LIMIT 50');
$q->execute([$staff['id']]);
json_out(['ok'=>true,'records'=>$q->fetchAll()]);
