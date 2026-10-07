# MMGen  
#### 手に持ったアイテムをMMアイテムにするプラグイン

## 概要  
Minecraft上で手に持った非MMアイテムの   
Display、Lore、CustomModelData値を読み取り、.ymlを生成  
それをMMのフォルダにぶち込めばMMアイテムの完成。  
※直接MMアイテムにするものではありません。  

このプラグインで作成したMMアイテムはデフォルトで  
空スキル(delay 0)を追加します。そのため、/mmidでもMMIDが判別できるようになります。 


## コマンド集
### `/mmgen items create newyml:<name>`  
/plugins/MMGen/items/ に`<name>.yml`が生成されます。  
例：/mmgen items create newyml:kurosio-test  
→kurosio-test.ymlが作成されます。(中身は空)  

### `/mmgen items create newid:<newMMID> [filename:<filename.yml>] [options:<option>] [acm:<number>] `  
手に持ったアイテムからMMのアイテムをつくります。  
  
- `newid:<newMMID>`  
`<newMMID>`にあなたが新しく考えたMMIDを入力します。  

 
- `filename:<filename.yml>`  
`<filename.yml>`の部分に既存の.ymlを選択することで同じ.yml内に複数のMMアイテムの情報を記述していくことができます。  
※filenameを入力しなかった場合はnewMMIDと同じ名前の.yml、MMアイテムが生成・記述されます。  

 
- `options:<option>`Unbeakableを選択できます。これで不可解になります。   
Empty-skill-offで空スキルをつけないように設定できます。  

 
- `acm:<number>`デフォルトでは自動でCustomModelData値を読み取りしますが、入力することで別の番号を指定できます。
  
### `/mmgen items info`  
手に持っているアイテムの情報を表示します。  
#### 表示内容
- MCID(id)
- MMID
- Fileの場所  
- Display  
- CustomModelDataの値
- Lore

### `/mmgen items mmid`  
手に持っているMMアイテムのMMIDを表示します。  
従来の`/mmid`では取得できなかったアイテムも表示することができます。(info同様)  

### `/mmgen items insert-empty-skill`  
手に持っているMMアイテムに空スキルを挿入します。  
①スキルがついていないMMアイテムを手にもつ  
②コマンドを実行  
③ファイル場所やDIsplay等を確認する。  
④[メッセージクリック]で該当yml内が編集され、空スキル(delay 0)が付与  
⑤/mm reload実行で完成  
⑥/mm items giveコマンドで出して確認  
※同じDisplay、Lore等内部情報が同じの場合は付与できません。


## アイテム作成のながれ・導入
①MMGen-x.x.x.jarをPluginsフォルダ内にアップロード  
②サーバー内で`/plugman load MMGen-x.x.x` を実行  
③手にMMアイテムにしたいアイテムを持ちます  
④`/mmgen items create newyml<name>`を実行し、新規.ymlを生成します。  
　.ymlは/plugins/MMGen/items 内に生成されます。  
⑤`/mmgen items create newid:<newMMID> [filename:<filename.yml>] [options:<option>] `  
　を実行し、指定.ymlファイルの中にMMアイテムをつくっていきます。  
⑥できた.ymlファイルを/plugins/MythicMobs/Items/任意フォルダ  
　の中にコピペしていきます。  
⑦サーバー内で`/mm reload`を実行  
⑧MMアイテムが完成  

※コマンドを実行したあとでも手に持っているアイテムは非MMアイテムです。  
　コマンドを実行しても直接MMアイテムに変わるわけではありません。 

### 生成される.yml中身例
`k_fan1:`  
　`Id: GOLD_INGOT`  
　`Display: '&r&9&l扇風機'`  
　`Lore:`  
　`- '&r&r&f扇風機の説明'`  
  `Options:`  
  　`Unbreakable: true`  
  `Model: 30`  
`Skills:`  
　`delay 0`


### /mmgen items info でのMMID表示までの流れ  
①手元のアイテム情報を取得  
②MythicMobsのItemsフォルダ内を検索  
③YAMLファイルを1つずつ精査、手元のアイテムと内容を比較  
④条件が一致したMMIDを特定  
⑤MMIDに対応するYAMLファイルを検索  
⑥取得したDisplay,MMID,Model,Lore等の検索結果を表示  

  
### Licence
This project is licensed under the GNU General Public License v3.0 (GPL-3.0).

See the LICENSE file for details.  

### Softdependencies
- MythicMobs
　
