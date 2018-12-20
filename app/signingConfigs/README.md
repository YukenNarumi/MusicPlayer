このディレクトリ内にkeyやkeystoreの設定を記述した
gradleファイルを配置し、
下記のように参照する
[apply from: 'signingConfigs/"ファイル名".gradle', to: android]

また実際のファイルはコミット・プッシュしないこと