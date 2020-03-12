# lab
サイボウズ・ラボでの開発リポジトリ  
各プログラムのソースのみを配置しており、個別にプライベートリポジトリを設けています。  
最新版は、各プライベートリポジトリにあります。

# プログラム説明
## InstrumentTool
計装(トレース出力コード挿入)ツール  
MapReduce向けに設計中だが、他のJavaプログラムにも適応可能  
入力：クラスファイル  
出力：計装されたクラスファイル  
## HBGraphGenerator
グラフの到達可能性判定ツール  
[A compression technique to materialize transitive closure] [ACMTDS'90]のアルゴリズムに基づいた実装
## RMI
Java RMIのテストコード
## TextGenerator
Hadoop MapReduceのワードカウントに入力として与えるテキストファイル生成プログラム


<!-- リンク -->
[A compression technique to materialize transitive closure]: https://dl.acm.org/doi/10.1145/99935.99944