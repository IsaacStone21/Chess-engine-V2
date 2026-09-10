package GUI;

import EngineUtil.Engine;


public class Main {
 public static void main(String[] args) {
    //new LaunchPage();

    int numMoves;

    for (int depth = 0; depth < 8; depth++) {
      numMoves = Engine.getNumPossiblePositions(depth);
      System.out.println("Num Possible Positions after " + depth + " moves: " + numMoves);
    }
  }
 }