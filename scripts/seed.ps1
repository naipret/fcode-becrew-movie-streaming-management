$ErrorActionPreference = "Stop"

$dataDir = Join-Path $PSScriptRoot "..\data"
if (-not (Test-Path $dataDir)) {
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
}

$today = Get-Date
$fmt = "yyyy-MM-ddTHH:mm:ss"

$dNow = $today.ToString($fmt)
$dTodayMorn = $today.Date.AddHours(9).ToString($fmt)
$dYesterdayEve = $today.AddDays(-1).Date.AddHours(20).ToString($fmt)
$dPast2Days = $today.AddDays(-2).Date.AddHours(21).ToString($fmt)
$dPast3Days = $today.AddDays(-3).Date.AddHours(19).ToString($fmt)
$dPast4Days = $today.AddDays(-4).Date.AddHours(22).ToString($fmt)
$dPast5Days = $today.AddDays(-5).Date.AddHours(15).ToString($fmt)
$dPast6Days = $today.AddDays(-6).Date.AddHours(20).ToString($fmt)
$dPast8Days = $today.AddDays(-8).Date.AddHours(18).ToString($fmt)
$dPast10Days = $today.AddDays(-10).Date.AddHours(21).ToString($fmt)
$dPast12Days = $today.AddDays(-12).Date.AddHours(14).ToString($fmt)
$dPast15Days = $today.AddDays(-15).Date.AddHours(20).ToString($fmt)
$dPast18Days = $today.AddDays(-18).Date.AddHours(19).ToString($fmt)
$dPast20Days = $today.AddDays(-20).Date.AddHours(22).ToString($fmt)
$dPast25Days = $today.AddDays(-25).Date.AddHours(16).ToString($fmt)
$dPast28Days = $today.AddDays(-28).Date.AddHours(20).ToString($fmt)
$dPast35Days = $today.AddDays(-35).Date.AddHours(19).ToString($fmt)
$dPast45Days = $today.AddDays(-45).Date.AddHours(21).ToString($fmt)
$dPast60Days = $today.AddDays(-60).Date.AddHours(20).ToString($fmt)

$categoriesCsv = @"
id,name,description,isDeleted
G00001,Sci-Fi,Science fiction and futuristic technology,false
G00002,Action,High energy stunts combat and chases,false
G00003,Drama,Character driven realistic emotional narratives,false
G00004,Comedy,Humorous lighthearted and entertaining stories,false
G00005,Horror,Supernatural thrillers suspense and fear,false
G00006,Animation,Hand-drawn CGI and artistic animated features,false
G00007,Thriller,Psychological mysteries suspense and twists,false
G00008,Romance,Romantic relationships drama and intimacy,false
"@

$moviesCsv = @"
id,title,categoryId,director,cast,releaseYear,durationMinutes,rating,viewCount,isDeleted
M00001,Inception,G00001,Christopher Nolan,"Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page",2010,148,8.8,52000,false
M00002,Interstellar,G00001,Christopher Nolan,"Matthew McConaughey, Anne Hathaway, Jessica Chastain",2014,169,8.7,48000,false
M00003,The Matrix,G00001,Lana Wachowski,"Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss",1999,136,8.7,45000,false
M00004,Blade Runner 2049,G00001,Denis Villeneuve,"Ryan Gosling, Harrison Ford, Ana de Armas",2017,164,8.0,31000,false
M00005,The Dark Knight,G00002,Christopher Nolan,"Christian Bale, Heath Ledger, Aaron Eckhart",2008,152,9.0,68000,false
M00006,Mad Max Fury Road,G00002,George Miller,"Tom Hardy, Charlize Theron, Nicholas Hoult",2015,120,8.1,38000,false
M00007,John Wick,G00002,Chad Stahelski,"Keanu Reeves, Michael Nyqvist, Alfie Allen",2014,101,7.4,34000,false
M00008,Gladiator,G00002,Ridley Scott,"Russell Crowe, Joaquin Phoenix, Connie Nielsen",2000,155,8.5,41000,false
M00009,The Shawshank Redemption,G00003,Frank Darabont,"Tim Robbins, Morgan Freeman, Bob Gunton",1994,142,9.3,72000,false
M00010,The Godfather,G00003,Francis Ford Coppola,"Marlon Brando, Al Pacino, James Caan",1972,175,9.2,59000,false
M00011,Parasite,G00003,Bong Joon-ho,"Song Kang-ho, Lee Sun-kyun, Cho Yeo-jeong",2019,132,8.5,36000,false
M00012,The Grand Budapest Hotel,G00004,Wes Anderson,"Ralph Fiennes, F. Murray Abraham, Mathieu Amalric",2014,99,8.1,28000,false
M00013,Superbad,G00004,Greg Mottola,"Jonah Hill, Michael Cera, Christopher Mintz-Plasse",2007,113,7.6,25000,false
M00014,A Quiet Place,G00005,John Krasinski,"Emily Blunt, John Krasinski, Millicent Simmonds",2018,90,7.5,29000,false
M00015,Get Out,G00005,Jordan Peele,"Daniel Kaluuya, Allison Williams, Bradley Whitford",2017,104,7.8,33000,false
M00016,Spirited Away,G00006,Hayao Miyazaki,"Rumi Hiiragi, Miyu Irino, Mari Natsuki",2001,125,8.6,42000,false
M00017,Spider-Man Into the Spider-Verse,G00006,Bob Persichetti,"Shameik Moore, Jake Johnson, Hailee Steinfeld",2018,117,8.4,46000,false
M00018,Se7en,G00007,David Fincher,"Brad Pitt, Morgan Freeman, Gwyneth Paltrow",1995,127,8.6,47000,false
M00019,Shutter Island,G00007,Martin Scorsese,"Leonardo DiCaprio, Mark Ruffalo, Ben Kingsley",2010,138,8.2,39000,false
M00020,La La Land,G00008,Damien Chazelle,"Ryan Gosling, Emma Stone, John Legend",2016,128,8.0,35000,false
"@

$subscribersCsv = @"
id,fullName,phone,email,tier,isDeleted
U00001,Nguyen Van An,0901234567,an.nguyen@domain.com,Premium,false
U00002,Tran Thi Binh,0912345678,binh.tran@domain.com,Standard,false
U00003,Le Hoang Cuong,0923456789,cuong.le@domain.com,Premium,false
U00004,Pham Minh Duc,0934567890,duc.pham@domain.com,Standard,false
U00005,Hoang Thuy Linh,0945678901,linh.hoang@domain.com,Premium,false
U00006,Vu Tuan Kiet,0967890123,kiet.vu@domain.com,Standard,false
U00007,Doan Bao Ngoc,0978901234,ngoc.doan@domain.com,Premium,false
U00008,Bui Quoc Huy,0989012345,huy.bui@domain.com,Standard,false
U00009,Dang My Hanh,0909876543,hanh.dang@domain.com,Premium,false
U00010,Ngo Thanh Tung,0918765432,tung.ngo@domain.com,Standard,false
U00011,Dinh Gia Phuc,0927654321,phuc.dinh@domain.com,Premium,false
U00012,Cao Phuong Thao,0936543210,thao.cao@domain.com,Standard,false
U00013,Phan Huu Thinh,0945432109,thinh.phan@domain.com,Premium,false
U00014,Luong Kim Oanh,0964321098,oanh.luong@domain.com,Standard,false
U00015,Trinh Duc Manh,0973210987,manh.trinh@domain.com,Premium,false
"@

$sessionsCsv = @"
sessionId,subscriberId,movieId,sessionDate,watchedMinutes,isCompleted
S00001,U00001,M00001,$dTodayMorn,148,true
S00002,U00002,M00005,$dYesterdayEve,152,true
S00003,U00003,M00002,$dYesterdayEve,80,false
S00004,U00004,M00009,$dPast2Days,142,true
S00005,U00005,M00001,$dPast2Days,148,true
S00006,U00006,M00006,$dPast3Days,120,true
S00007,U00007,M00016,$dPast3Days,125,true
S00008,U00008,M00017,$dPast4Days,60,false
S00009,U00009,M00005,$dPast4Days,152,true
S00010,U00010,M00018,$dPast5Days,127,true
S00011,U00011,M00003,$dPast5Days,136,true
S00012,U00012,M00012,$dPast6Days,99,true
S00013,U00013,M00014,$dPast6Days,90,true
S00014,U00014,M00020,$dPast6Days,128,true
S00015,U00015,M00001,$dPast8Days,148,true
S00016,U00001,M00005,$dPast8Days,152,true
S00017,U00002,M00002,$dPast10Days,169,true
S00018,U00003,M00010,$dPast10Days,90,false
S00019,U00004,M00007,$dPast12Days,101,true
S00020,U00005,M00004,$dPast12Days,164,true
S00021,U00006,M00011,$dPast15Days,132,true
S00022,U00007,M00015,$dPast15Days,104,true
S00023,U00008,M00019,$dPast18Days,70,false
S00024,U00009,M00008,$dPast18Days,155,true
S00025,U00010,M00013,$dPast20Days,113,true
S00026,U00011,M00016,$dPast20Days,125,true
S00027,U00012,M00003,$dPast25Days,136,true
S00028,U00013,M00009,$dPast25Days,142,true
S00029,U00014,M00005,$dPast28Days,152,true
S00030,U00015,M00001,$dPast28Days,148,true
S00031,U00001,M00002,$dPast35Days,169,true
S00032,U00002,M00006,$dPast35Days,120,true
S00033,U00003,M00018,$dPast45Days,127,true
S00034,U00004,M00010,$dPast45Days,175,true
S00035,U00005,M00007,$dPast60Days,101,true
S00036,U00006,M00017,$dPast60Days,117,true
S00037,U00007,M00008,$dPast60Days,155,true
S00038,U00008,M00004,$dPast60Days,164,true
S00039,U00009,M00011,$dPast60Days,132,true
S00040,U00010,M00020,$dPast60Days,128,true
"@

$utf8NoBom = New-Object System.Text.UTF8Encoding $false

[System.IO.File]::WriteAllText((Join-Path $dataDir "categories.csv"), $categoriesCsv.Trim() + "`n", $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $dataDir "movies.csv"), $moviesCsv.Trim() + "`n", $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $dataDir "subscribers.csv"), $subscribersCsv.Trim() + "`n", $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $dataDir "sessions.csv"), $sessionsCsv.Trim() + "`n", $utf8NoBom)

Write-Host "Demo data generated successfully in $dataDir" -ForegroundColor Green
Write-Host "  - Categories : 8 genres"
Write-Host "  - Movies     : 20 titles"
Write-Host "  - Subscribers: 15 accounts (Standard and Premium)"
Write-Host "  - Sessions   : 40 sessions with dynamic relative timestamps"
