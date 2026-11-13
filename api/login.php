<?php
require_once __DIR__.'/../db.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_out(['ok'=>false,'error'=>'POST required'],405);
$in=json_decode(file_get_contents('php://input'),true) ?: [];
$code=trim((string)($in['staff_code']??'')); $password=(string)($in['password']??'');
if ($code==='' || $password==='') json_out(['ok'=>false,'error'=>'Staff Code and password are required'],422);
$q=db()->prepare('SELECT id,staff_code,full_name,branch,password_hash,active FROM staff WHERE staff_code=? LIMIT 1');
$q->execute([$code]); $s=$q->fetch();
if (!$s || (int)$s['active']!==1 || !password_verify($password,(string)$s['password_hash'])) json_out(['ok'=>false,'error'=>'Invalid staff code or password'],401);
$token=bin2hex(random_bytes(32)); $expires=(new DateTimeImmutable('+14 days'))->format('Y-m-d H:i:s');
$q=db()->prepare('INSERT INTO api_tokens (staff_id,token_hash,expires_at,created_at) VALUES (?,?,?,NOW())');
$q->execute([$s['id'],hash('sha256',$token),$expires]);
json_out(['ok'=>true,'token'=>$token,'staff'=>['id'=>(int)$s['id'],'staff_code'=>$s['staff_code'],'name'=>$s['full_name'],'branch'=>$s['branch']]]);
