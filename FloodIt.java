import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import tester.Tester;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// Represents a single square of the game area
class Cell {
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  Color color;
  boolean flooded;

  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  Cell(Color color) {
    this.color = color;
    this.flooded = false;
  }

  // EFFECT: sets the cell's top cell to the given cell
  void setTop(Cell top) {
    this.top = top;
  }

  // EFFECT: sets the cell's bottom cell to the given cell
  void setBottom(Cell bottom) {
    this.bottom = bottom;
  }

  // EFFECT: sets the cell's left cell to the given cell
  void setLeft(Cell left) {
    this.left = left;
  }

  // EFFECT: sets the cell's right cell to the given cell
  void setRight(Cell right) {
    this.right = right;
  }

  // EFFECT: floods the cell on call
  void flooded() {
    this.flooded = true;
  }

  // EFFECT: floods the neighbors of a flooded cell that have the given flood
  // color
  void neighborWithColor(Color color) {
    if (this.flooded) {
      if (left != null && left.color.equals(color)) {
        this.left.flooded();
      }
      if (right != null && right.color.equals(color)) {
        this.right.flooded();
      }
      if (top != null && top.color.equals(color)) {
        this.top.flooded();
      }
      if (bottom != null && bottom.color.equals(color)) {
        this.bottom.flooded();
      }
    }
  }
}

// represents the class FloodItWorld
class FloodItWorld extends World {
  // All the cells of the game
  ArrayList<ArrayList<Cell>> board;

  int sideLength;

  Random rand;

  final int CELL_SIZE = 25;
  Color floodColor;

  int numColors;
  ArrayList<Color> colorOptions = new ArrayList<Color>();

  WorldScene gameBoard;
  final int BOARD_POSITION_IN_WINDOW = 250;

  // fields for waterfalling
  ArrayList<ArrayList<Cell>> propogateOrder;
  int propogateIndex = 0;
  int userClicksCount = 0;
  boolean currentlyWaterfalling = false;

  int maxClicksAllowed;

  // two arg constructor --- not seeded
  FloodItWorld(int sideLength, int numColors) {
    this.sideLength = sideLength;
    this.numColors = numColors;

    this.rand = new Random();

    gameBoard = new WorldScene(500, 500);

    this.maxClicksAllowed = (int) (this.sideLength + 2 * this.numColors + 3);
    if (this.maxClicksAllowed < 5) {
      this.maxClicksAllowed = 5;
    }
    if (this.maxClicksAllowed > 100) {
      this.maxClicksAllowed = 100;
    }
  }

  // two arg constructor -- seeded
  FloodItWorld(int sideLength, int numColors, int seed) {
    this(sideLength, numColors);
    this.rand = new Random(seed);

  }

  // EFFECT: inits color options
  void initColors() {
    for (int i = 0; i < this.numColors; i++) {
      int r = this.rand.nextInt(256);
      int g = this.rand.nextInt(256);
      int b = this.rand.nextInt(256);
      Color newColor = new Color(r, g, b);
      this.colorOptions.add(newColor);
      System.out.println(r + " " + g + " " + b);
    }
  }

  // EFFECT: inits cell neighbors
  void initNeighbors() {
    for (int i = 0; i < board.size(); i++) {
      for (int j = 0; j < board.get(i).size(); j++) {
        // this cell
        Cell cell = board.get(i).get(j);

        // Assign top neighbor
        if (i > 0) {
          cell.setTop(board.get(i - 1).get(j));
        }

        // Assign bottom neighbor
        if (i < board.size() - 1) {
          cell.setBottom(board.get(i + 1).get(j));
        }

        // Assign left neighbor
        if (j > 0) {
          cell.setLeft(board.get(i).get(j - 1));
        }

        // Assign right neighbor
        if (j < board.get(i).size() - 1) {
          cell.setRight(board.get(i).get(j + 1));
        }
      }
    }

    // checks if the list of cell neighbors starts off with neighboring same colors
    for (ArrayList<Cell> row : this.board) {
      for (Cell cell : row) {
        cell.neighborWithColor(this.floodColor);
      }
    }
  }

  // EFFECT: initializes the board with a user given size, populating each with a
  // new cell
  // of a random color
  void initBoard() {
    this.initColors();
    this.board = new ArrayList<ArrayList<Cell>>();
    // 2d loops to populate table array
    for (int i = 0; i < this.sideLength; i++) {
      ArrayList<Cell> row = new ArrayList<Cell>();
      for (int j = 0; j < this.sideLength; j++) {
        Color randomCellColor = this.colorOptions.get(rand.nextInt(this.numColors));
        Cell randomCell = new Cell(randomCellColor);
        row.add(randomCell);
      }
      board.add(row);
    }

    // set starting cell flood color
    // and set the first cell to already be flooded
    this.floodColor = this.board.get(0).get(0).color;
    this.board.get(0).get(0).flooded();

    this.propogateOrder = this.propogateOrder();
  }

  // returns arraylist with pattern for propogation -- responsible for waterfall
  // animation order
  ArrayList<ArrayList<Cell>> propogateOrder() {
    ArrayList<ArrayList<Cell>> propOrder = new ArrayList<ArrayList<Cell>>();

    for (int i = 0; i < this.sideLength; i++) {
      // get all row items add them to list
      ArrayList<Cell> radiusCells = new ArrayList<Cell>();
      for (int j = 0; j <= i; j++) {
        if (j == i) {
          radiusCells.add(this.board.get(i).get(j));
        }
        else {
          radiusCells.add(this.board.get(i).get(j));
          radiusCells.add(this.board.get(j).get(i));
        }
      }
      propOrder.add(radiusCells);
    }

    return propOrder;
  }

  // returns true if the whole board is flooded
  boolean wholeBoardFlooded() {
    boolean allFlooded = true;

    for (ArrayList<Cell> row : this.board) {
      for (Cell cell : row) {
        if (!cell.flooded) {
          allFlooded = false;
        }
      }
    }
    return allFlooded;
  }

  // draws board in its current state
  WorldImage drawBoard() {
    WorldImage tempBoard = new EmptyImage();
    for (ArrayList<Cell> row : this.board) {
      WorldImage rowImage = new EmptyImage();
      for (Cell cell : row) {
        WorldImage cellImage = new RectangleImage(this.CELL_SIZE, this.CELL_SIZE, OutlineMode.SOLID,
            cell.color);
        rowImage = new BesideImage(rowImage, cellImage);
      }
      tempBoard = new AboveImage(tempBoard, rowImage);
    }
    return tempBoard;
  }

  // creates image of flood it world
  public WorldScene makeScene() {
    this.onTick();
    gameBoard.placeImageXY(new TextImage("Flood It", 30, Color.black), 250, 40);
    gameBoard.placeImageXY(new RectangleImage(100, 100, OutlineMode.SOLID, Color.white), 250, 450);
    gameBoard.placeImageXY(
        new TextImage(this.userClicksCount + "/" + this.maxClicksAllowed, 30, Color.black), 250,
        450);
    gameBoard.placeImageXY(this.drawBoard(), this.BOARD_POSITION_IN_WINDOW,
        this.BOARD_POSITION_IN_WINDOW);
    return gameBoard;
  }

  // returns the pressed cell based on given position
  Cell pressedCell(Posn pos) {

    // gets x and y pos in game board
    int xPos = pos.x - (this.BOARD_POSITION_IN_WINDOW - this.CELL_SIZE * this.sideLength / 2);
    int yPos = pos.y - (this.BOARD_POSITION_IN_WINDOW - this.CELL_SIZE * this.sideLength / 2);

    // index of column and row
    int columnIndex = xPos / this.CELL_SIZE;
    int rowIndex = yPos / this.CELL_SIZE;

    // returns cell pressed
    return this.board.get(rowIndex).get(columnIndex);
  }

  // handles ticking of the clock and updating the flood it world if needed
  public void onTick() {
    // this means that there is still more waterfalling to do, so the user shouldnt
    // be able to click another button
    if (this.currentlyWaterfalling) {

      ArrayList<Cell> cellsToPotentiallyChange = this.propogateOrder.get(this.propogateIndex);
      for (Cell cell : cellsToPotentiallyChange) {
        if (cell.flooded) {
          cell.color = this.floodColor;
        }
      }

      if (this.propogateIndex == this.propogateOrder.size() - 1) {
        this.currentlyWaterfalling = false;
        this.propogateIndex = 0;

        // this means the user has won the game
        if (this.wholeBoardFlooded()) {
          this.endScreen("You Win");
        }

        // this means user has lost the game
        if (this.userClicksCount >= this.maxClicksAllowed) {
          this.endScreen("You Lose");
        }
      }
      else {
        this.propogateIndex++;
      }
    }
  }

  // EFFECT: modifies gameBoard, drawing last scene when user wins or loses
  void endScreen(String msg) {
    this.gameBoard = new WorldScene(500, 500);
    this.gameBoard.placeImageXY(new TextImage(msg, 30, Color.black), 250, 70);
  }

  // EFFECT: changes user clicks count in response to key press events
  public void onKeyEvent(String key) {

    // restarts game on r pressed
    if (key.equals("r")) {

      System.out.println("hi");

      this.gameBoard = new WorldScene(500, 500);

      // set up board
      this.initColors();
      this.initBoard();
      this.initNeighbors();

      this.userClicksCount = 0;
      this.maxClicksAllowed = (int) (this.sideLength + 2 * this.numColors + 3);
      if (this.maxClicksAllowed < 5) {
        this.maxClicksAllowed = 5;
      }
      if (this.maxClicksAllowed > 100) {
        this.maxClicksAllowed = 100;
      }
    }
  }

  // EFFECT: handles mouse clicks with given mouse location and updates the
  // userClicksCount and water falling
  public void onMouseClicked(Posn pos) {

    // if user pressed within bounds of game board
    if (!this.currentlyWaterfalling
        && pos.x > this.BOARD_POSITION_IN_WINDOW - this.CELL_SIZE * this.sideLength / 2
        && pos.x < this.BOARD_POSITION_IN_WINDOW + this.CELL_SIZE * this.sideLength / 2
        && pos.y > this.BOARD_POSITION_IN_WINDOW - this.CELL_SIZE * this.sideLength / 2
        && pos.y < this.BOARD_POSITION_IN_WINDOW + this.CELL_SIZE * this.sideLength / 2) {

      Color newFloodColor = this.pressedCell(pos).color;

      if (newFloodColor != this.floodColor) {
        this.floodColor = newFloodColor;

        // go through all of the cells on the board
        // if any of them are flooded already, check its neighbors
        for (ArrayList<Cell> row : this.board) {
          for (Cell cell : row) {
            cell.neighborWithColor(this.floodColor);
          }
        }
        this.userClicksCount++;
        this.currentlyWaterfalling = true;
      }

    }
  }

}

// examples class of FloodIt
class ExamplesFloodIts {
  FloodItWorld world1;
  FloodItWorld world2;
  FloodItWorld world3;
  FloodItWorld world4;

  // represent examples of cells in the board
  Cell firstCell = new Cell(new Color(207, 235, 55));
  Cell secondCell = new Cell(new Color(187, 232, 235));
  Cell thirdCell = new Cell(new Color(173, 72, 20));
  Cell fourthCell = new Cell(new Color(173, 72, 20));

  // to initiate all example FloodItWorlds used in tests and their fields
  void initTestExamples() {
    this.world1 = new FloodItWorld(3, 7, 2);
    this.world2 = new FloodItWorld(3, 7, 2);
    this.world3 = new FloodItWorld(2, 3, 4);
    this.world4 = new FloodItWorld(2, 3, 4);

    world1.initColors();
    world1.initBoard();
    world1.initNeighbors();

    world2.initColors();
    world2.initBoard();
    world2.initNeighbors();

    world3.initColors();
    world3.initBoard();
    world3.initNeighbors();

    world4.initColors();
    world4.initBoard();
    world4.initNeighbors();
  }

  // test methods for the Cell class
  // to test the method setTop
  void testSetTop(Tester t) {
    this.initTestExamples();

    world1.board.get(2).get(0).setTop(firstCell);
    t.checkExpect(world1.board.get(2).get(0).top, firstCell);

    world2.board.get(0).get(1).setTop(secondCell);
    t.checkExpect(world2.board.get(0).get(1).top, secondCell);

    world3.board.get(1).get(0).setTop(thirdCell);
    t.checkExpect(world3.board.get(1).get(0).top, thirdCell);

    world4.board.get(0).get(0).setTop(fourthCell);
    t.checkExpect(world4.board.get(0).get(0).top, fourthCell);
  }

  // to test the method setBottom
  void testSetBottom(Tester t) {
    this.initTestExamples();

    world1.board.get(2).get(0).setBottom(firstCell);
    t.checkExpect(world1.board.get(2).get(0).bottom, firstCell);

    world2.board.get(0).get(1).setBottom(secondCell);
    t.checkExpect(world2.board.get(0).get(1).bottom, secondCell);

    world3.board.get(1).get(0).setBottom(thirdCell);
    t.checkExpect(world3.board.get(1).get(0).bottom, thirdCell);

    world4.board.get(0).get(0).setBottom(fourthCell);
    t.checkExpect(world4.board.get(0).get(0).bottom, fourthCell);
  }

  // to test the method setLeft
  void testSetLeft(Tester t) {
    this.initTestExamples();

    world1.board.get(2).get(0).setLeft(firstCell);
    t.checkExpect(world1.board.get(2).get(0).left, firstCell);

    world2.board.get(0).get(1).setLeft(secondCell);
    t.checkExpect(world2.board.get(0).get(1).left, secondCell);

    world3.board.get(1).get(0).setLeft(thirdCell);
    t.checkExpect(world3.board.get(1).get(0).left, thirdCell);

    world4.board.get(0).get(0).setLeft(fourthCell);
    t.checkExpect(world4.board.get(0).get(0).left, fourthCell);
  }

  // to test the method setRight
  void testSetRight(Tester t) {
    this.initTestExamples();

    world1.board.get(2).get(0).setRight(firstCell);
    t.checkExpect(world1.board.get(2).get(0).right, firstCell);

    world2.board.get(0).get(1).setRight(secondCell);
    t.checkExpect(world2.board.get(0).get(1).right, secondCell);

    world3.board.get(1).get(0).setRight(thirdCell);
    t.checkExpect(world3.board.get(1).get(0).right, thirdCell);

    world4.board.get(0).get(0).setRight(fourthCell);
    t.checkExpect(world4.board.get(0).get(0).right, fourthCell);
  }

  // to test the method flooded
  void testFlooded(Tester t) {
    this.initTestExamples();
    world1.board.get(0).get(0).flooded();
    t.checkExpect(world1.board.get(0).get(0).flooded, true);

    world2.board.get(2).get(1).flooded();
    t.checkExpect(world2.board.get(2).get(1).flooded, true);

    world3.board.get(1).get(1).flooded();
    t.checkExpect(world3.board.get(1).get(1).flooded, true);

    world4.board.get(0).get(1).flooded();
    t.checkExpect(world4.board.get(0).get(1).flooded, true);
  }

  // to test the method neighborWithColor
  void testNeighborWithColor(Tester t) {
    this.initTestExamples();

    FloodItWorld testWorld = new FloodItWorld(3, 3, 234);
    testWorld.initColors();
    testWorld.initBoard();
    testWorld.initNeighbors();
    t.checkExpect(testWorld.board.get(0).get(1).flooded, false);
    t.checkExpect(testWorld.board.get(1).get(0).flooded, true);
    testWorld.board.get(0).get(0).neighborWithColor(new Color(199, 207, 102));
    t.checkExpect(testWorld.board.get(0).get(1).flooded, true);
    t.checkExpect(testWorld.board.get(1).get(1).right.flooded, false);
    t.checkExpect(testWorld.board.get(1).get(1).bottom.flooded, false);
    testWorld.board.get(1).get(1).flooded();
    testWorld.board.get(1).get(1).neighborWithColor(new Color(54, 7, 248));
    t.checkExpect(testWorld.board.get(1).get(1).right.flooded, true);
    t.checkExpect(testWorld.board.get(1).get(1).bottom.flooded, true);

    FloodItWorld testWorld2 = new FloodItWorld(5, 4, 244);
    testWorld2.initColors();
    testWorld2.initBoard();
    testWorld2.initNeighbors();
    System.out.println(testWorld2.board.get(0).get(2).color);
    t.checkExpect(testWorld2.board.get(0).get(0).flooded, true);
    t.checkExpect(testWorld2.board.get(0).get(1).flooded, false);
    t.checkExpect(testWorld2.board.get(1).get(0).flooded, true);
    t.checkExpect(testWorld2.board.get(1).get(1).flooded, false);
    testWorld2.board.get(0).get(0).neighborWithColor(new Color(91, 139, 80));
    t.checkExpect(testWorld2.board.get(0).get(0).flooded, true);
    t.checkExpect(testWorld2.board.get(0).get(1).flooded, true);
    t.checkExpect(testWorld2.board.get(1).get(0).flooded, true);
    t.checkExpect(testWorld2.board.get(1).get(1).flooded, false);
    testWorld2.board.get(0).get(1).neighborWithColor(new Color(229, 185, 167));
    t.checkExpect(testWorld2.board.get(0).get(2).flooded, true);
  }

  // test methods for the FloodItWorld class
  // tests the method initColors
  void testInitColors(Tester t) {
    this.initTestExamples();

    t.checkExpect(world1.colorOptions,
        new ArrayList<Color>(Arrays.asList(new Color(187, 75, 230), new Color(1, 127, 218),
            new Color(252, 8, 219), new Color(236, 252, 115), new Color(58, 133, 19),
            new Color(215, 190, 63), new Color(243, 141, 7), new Color(28, 177, 201),
            new Color(218, 96, 14), new Color(70, 104, 242), new Color(90, 133, 254),
            new Color(172, 143, 85), new Color(121, 203, 12), new Color(43, 149, 42))));

    t.checkExpect(world2.colorOptions,
        new ArrayList<Color>(Arrays.asList(new Color(187, 75, 230), new Color(1, 127, 218),
            new Color(252, 8, 219), new Color(236, 252, 115), new Color(58, 133, 19),
            new Color(215, 190, 63), new Color(243, 141, 7), new Color(28, 177, 201),
            new Color(218, 96, 14), new Color(70, 104, 242), new Color(90, 133, 254),
            new Color(172, 143, 85), new Color(121, 203, 12), new Color(43, 149, 42))));

    t.checkExpect(world3.colorOptions,
        new ArrayList<Color>(
            Arrays.asList(new Color(187, 232, 235), new Color(207, 235, 55), new Color(173, 72, 20),
                new Color(127, 6, 218), new Color(179, 204, 206), new Color(68, 38, 147))));

    t.checkExpect(world4.colorOptions,
        new ArrayList<Color>(
            Arrays.asList(new Color(187, 232, 235), new Color(207, 235, 55), new Color(173, 72, 20),
                new Color(127, 6, 218), new Color(179, 204, 206), new Color(68, 38, 147))));
  }

  //tests the method initBoard
  void testInitBoard(Tester t) {
    this.initTestExamples();

    firstCell.flooded();
    firstCell.setRight(secondCell);
    firstCell.setBottom(thirdCell);
    secondCell.setLeft(firstCell);
    secondCell.setBottom(fourthCell);
    thirdCell.setTop(firstCell);
    thirdCell.setRight(fourthCell);
    fourthCell.setTop(secondCell);
    fourthCell.setLeft(thirdCell);

    t.checkExpect(world3.board,
        new ArrayList<ArrayList<Cell>>(
            Arrays.asList(new ArrayList<Cell>(Arrays.asList(firstCell, secondCell)),

                new ArrayList<Cell>(Arrays.asList(thirdCell, fourthCell)))));

    t.checkExpect(world4.board,
        new ArrayList<ArrayList<Cell>>(
            Arrays.asList(new ArrayList<Cell>(Arrays.asList(firstCell, secondCell)),

                new ArrayList<Cell>(Arrays.asList(thirdCell, fourthCell)))));
  }

  // tests the method initNeighbors
  void testInitNeighbors(Tester t) {
    this.initTestExamples();

    t.checkExpect(world1.board.get(0).get(0).left, null);
    t.checkExpect(world1.board.get(0).get(0).top, null);
    t.checkExpect(world1.board.get(0).get(0).right, world1.board.get(0).get(1));
    t.checkExpect(world1.board.get(0).get(0).bottom, world1.board.get(1).get(0));

    t.checkExpect(world2.board.get(1).get(2).left, world2.board.get(1).get(1));
    t.checkExpect(world2.board.get(1).get(2).top, world2.board.get(0).get(2));
    t.checkExpect(world2.board.get(1).get(2).right, null);
    t.checkExpect(world2.board.get(1).get(2).bottom, world2.board.get(2).get(2));

    t.checkExpect(world3.board.get(1).get(0).left, null);
    t.checkExpect(world3.board.get(1).get(0).top, world3.board.get(0).get(0));
    t.checkExpect(world3.board.get(1).get(0).right, world3.board.get(1).get(1));
    t.checkExpect(world3.board.get(1).get(0).bottom, null);

    t.checkExpect(world4.board.get(1).get(1).left, world3.board.get(1).get(0));
    t.checkExpect(world4.board.get(1).get(1).top, world3.board.get(0).get(1));
    t.checkExpect(world4.board.get(1).get(1).right, null);
    t.checkExpect(world4.board.get(1).get(1).bottom, null);
  }

  // tests propogate order method
  void testPropogateOrder(Tester t) {
    this.initTestExamples();

    FloodItWorld test1 = new FloodItWorld(2, 5, 2);
    test1.initBoard();
    ArrayList<ArrayList<Cell>> outputTest1 = new ArrayList<ArrayList<Cell>>();
    ArrayList<Cell> wave1 = new ArrayList<Cell>(Arrays.asList(test1.board.get(0).get(0)));
    outputTest1.add(wave1);

    ArrayList<Cell> wave2 = new ArrayList<Cell>(Arrays.asList(test1.board.get(1).get(0),
        test1.board.get(0).get(1), test1.board.get(1).get(1)));
    outputTest1.add(wave2);
    t.checkExpect(test1.propogateOrder(), outputTest1);
    FloodItWorld test2 = new FloodItWorld(3, 5, 2);
    test2.initBoard();

    ArrayList<ArrayList<Cell>> outputTest2 = new ArrayList<ArrayList<Cell>>();
    ArrayList<Cell> wave3 = new ArrayList<Cell>(Arrays.asList(test2.board.get(0).get(0)));
    outputTest2.add(wave3);
    ArrayList<Cell> wave4 = new ArrayList<Cell>(Arrays.asList(test2.board.get(1).get(0),
        test2.board.get(0).get(1), test2.board.get(1).get(1)));
    outputTest2.add(wave4);
    ArrayList<Cell> wave5 = new ArrayList<Cell>(
        Arrays.asList(test2.board.get(2).get(0), test2.board.get(0).get(2),
            test2.board.get(2).get(1), test2.board.get(1).get(2), test2.board.get(2).get(2)));
    outputTest2.add(wave5);
    t.checkExpect(test2.propogateOrder(), outputTest2);

    FloodItWorld test3 = new FloodItWorld(4, 5, 2);
    test3.initBoard();
    ArrayList<ArrayList<Cell>> outputTest3 = new ArrayList<ArrayList<Cell>>();
    ArrayList<Cell> wave6 = new ArrayList<Cell>(Arrays.asList(test3.board.get(0).get(0)));
    outputTest3.add(wave6);
    ArrayList<Cell> wave7 = new ArrayList<Cell>(Arrays.asList(test3.board.get(1).get(0),
        test3.board.get(0).get(1), test3.board.get(1).get(1)));
    outputTest3.add(wave7);
    ArrayList<Cell> wave8 = new ArrayList<Cell>(
        Arrays.asList(test3.board.get(2).get(0), test3.board.get(0).get(2),
            test3.board.get(2).get(1), test3.board.get(1).get(2), test3.board.get(2).get(2)));
    outputTest3.add(wave8);
    ArrayList<Cell> wave9 = new ArrayList<Cell>(Arrays.asList(test3.board.get(3).get(0),
        test3.board.get(0).get(3), test3.board.get(3).get(1), test3.board.get(1).get(3),
        test3.board.get(3).get(2), test3.board.get(2).get(3), test3.board.get(3).get(3)));
    outputTest3.add(wave9);
    t.checkExpect(test3.propogateOrder(), outputTest3);
  }

  // to test the method wholeBoardFlooded
  void testWholeBoardFlooded(Tester t) {
    this.initTestExamples();

    t.checkExpect(world1.wholeBoardFlooded(), false);
    t.checkExpect(world2.wholeBoardFlooded(), false);
    t.checkExpect(world3.wholeBoardFlooded(), false);
    t.checkExpect(world4.wholeBoardFlooded(), false);

    firstCell.flooded();
    firstCell.setRight(secondCell);
    firstCell.setBottom(thirdCell);
    secondCell.setLeft(firstCell);
    secondCell.setBottom(fourthCell);
    thirdCell.setTop(firstCell);
    thirdCell.setRight(fourthCell);
    fourthCell.setTop(secondCell);
    fourthCell.setLeft(thirdCell);

    secondCell.flooded();
    thirdCell.flooded();
    fourthCell.flooded();

    FloodItWorld world5 = new FloodItWorld(2, 7, 2);
    ArrayList<ArrayList<Cell>> boardList1 = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(new ArrayList<Cell>(Arrays.asList(firstCell, secondCell)),
            new ArrayList<Cell>(Arrays.asList(thirdCell, fourthCell))));

    world5.board = boardList1;

    t.checkExpect(world5.wholeBoardFlooded(), true);

    firstCell.flooded();
    firstCell.setRight(thirdCell);
    firstCell.setBottom(secondCell);
    thirdCell.setLeft(firstCell);
    thirdCell.setBottom(fourthCell);
    secondCell.setTop(firstCell);
    secondCell.setRight(fourthCell);
    fourthCell.setTop(thirdCell);
    fourthCell.setLeft(secondCell);

    secondCell.flooded();
    thirdCell.flooded();
    fourthCell.flooded();

    FloodItWorld world6 = new FloodItWorld(2, 7, 2);
    ArrayList<ArrayList<Cell>> boardList2 = new ArrayList<ArrayList<Cell>>(
        Arrays.asList(new ArrayList<Cell>(Arrays.asList(firstCell, thirdCell)),
            new ArrayList<Cell>(Arrays.asList(secondCell, fourthCell))));

    world6.board = boardList2;

    t.checkExpect(world6.wholeBoardFlooded(), true);
  }

  // tests the method drawBoard
  void testDrawBoard(Tester t) {
    this.initTestExamples();

    t.checkExpect(this.world1.drawBoard(), new AboveImage(
        new AboveImage(new AboveImage(new EmptyImage(),

            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new RectangleImage(25, 25, OutlineMode.SOLID, new Color(187, 75, 230))),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(243, 141, 7))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(58, 133, 19)))),

            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new RectangleImage(25, 25, OutlineMode.SOLID, new Color(1, 127, 218))),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(252, 8, 219))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(1, 127, 218)))),

        new BesideImage(
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(215, 190, 63))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(187, 75, 230))),
            new RectangleImage(25, 25, OutlineMode.SOLID, new Color(243, 141, 7)))));

    t.checkExpect(this.world2.drawBoard(), new AboveImage(
        new AboveImage(new AboveImage(new EmptyImage(),

            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new RectangleImage(25, 25, OutlineMode.SOLID, new Color(187, 75, 230))),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(243, 141, 7))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(58, 133, 19)))),

            new BesideImage(
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new RectangleImage(25, 25, OutlineMode.SOLID, new Color(1, 127, 218))),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(252, 8, 219))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(1, 127, 218)))),

        new BesideImage(
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(215, 190, 63))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(187, 75, 230))),
            new RectangleImage(25, 25, OutlineMode.SOLID, new Color(243, 141, 7)))));

    t.checkExpect(this.world3.drawBoard(),
        new AboveImage(
            new AboveImage(new EmptyImage(),
                new BesideImage(
                    new BesideImage(new EmptyImage(),
                        new RectangleImage(25, 25, OutlineMode.SOLID, new Color(207, 235, 55))),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(187, 232, 235)))),
            new BesideImage(
                new BesideImage(new EmptyImage(),
                    new RectangleImage(25, 25, OutlineMode.SOLID, new Color(173, 72, 20))),
                new RectangleImage(25, 25, OutlineMode.SOLID, new Color(173, 72, 20)))));
  }

  // tests the method makeScene
  void testMakeScene(Tester t) {
    this.initTestExamples();

    this.world1.gameBoard.placeImageXY(new TextImage("Flood It", 30, Color.black), 250, 40);
    this.world1.gameBoard.placeImageXY(new RectangleImage(100, 100, OutlineMode.SOLID, Color.white),
        250, 450);
    this.world1.gameBoard.placeImageXY(new TextImage(0 + "/" + 20, 30, Color.black), 250, 450);
    this.world1.gameBoard.placeImageXY(world1.drawBoard(), 250, 250);
    t.checkExpect(world2.makeScene(), world1.gameBoard);

    this.world3.gameBoard.placeImageXY(new TextImage("Flood It", 30, Color.black), 250, 40);
    this.world3.gameBoard.placeImageXY(new RectangleImage(100, 100, OutlineMode.SOLID, Color.white),
        250, 450);
    this.world3.gameBoard.placeImageXY(new TextImage(0 + "/" + 11, 30, Color.black), 250, 450);
    this.world3.gameBoard.placeImageXY(world3.drawBoard(), 250, 250);
    t.checkExpect(world4.makeScene(), world3.gameBoard);
  }

  // tests pressed cell method
  void testPressedCell(Tester t) {
    this.initTestExamples();

    FloodItWorld testWorld = new FloodItWorld(2, 7, 2);
    Cell firstCell = new Cell(new Color(215, 190, 63));
    firstCell.flooded = true;
    testWorld.initBoard();

    FloodItWorld testWorld2 = new FloodItWorld(3, 7, 2);
    testWorld2.initBoard();
    Cell firstCell2 = new Cell(new Color(215, 190, 63));
    firstCell2.flooded = true;
    t.checkExpect(testWorld.pressedCell(new Posn(236, 240)), firstCell);
    t.checkExpect(testWorld.pressedCell(new Posn(237, 266)), new Cell(new Color(215, 190, 63)));
    t.checkExpect(testWorld.pressedCell(new Posn(265, 262)), new Cell(new Color(187, 75, 230)));
    t.checkExpect(testWorld.pressedCell(new Posn(266, 233)), new Cell(new Color(252, 8, 219)));
    t.checkExpect(testWorld2.pressedCell(new Posn(225, 225)), firstCell2);
    t.checkExpect(testWorld2.pressedCell(new Posn(275, 252)), new Cell(new Color(252, 8, 219)));
    t.checkExpect(testWorld2.pressedCell(new Posn(252, 275)), new Cell(new Color(187, 75, 230)));

  }

  // tests the onTick method
  void testOnTick(Tester t) {
    this.initTestExamples();

    FloodItWorld test1 = new FloodItWorld(3, 3, 5);
    test1.initBoard();
    test1.initColors();
    test1.initNeighbors();
    ArrayList<ArrayList<Cell>> boardGrab = test1.board;
    test1.propogateOrder = test1.propogateOrder();
    test1.currentlyWaterfalling = true;
    test1.floodColor = new Color(200, 0, 0);
    test1.maxClicksAllowed = 3;
    t.checkExpect(test1.board.get(0).get(0).color, new Color(187, 45, 22));
    t.checkExpect(test1.board.get(1).get(1).color, new Color(187, 45, 22));
    t.checkExpect(test1.board.get(2).get(2).color, new Color(156, 125, 241));
    test1.onTick();
    t.checkExpect(test1.board, boardGrab);
    t.checkExpect(test1.propogateIndex, 1);
    t.checkExpect(test1.currentlyWaterfalling, true);
    t.checkExpect(test1.floodColor, new Color(200, 0, 0));
    t.checkExpect(test1.board.get(1).get(1).color, new Color(187, 45, 22));
    t.checkExpect(test1.board.get(2).get(2).color, new Color(156, 125, 241));
    test1.board.get(1).get(1).flooded();
    test1.board.get(2).get(2).flooded();
    test1.onTick();
    t.checkExpect(test1.propogateIndex, 2);
    t.checkExpect(test1.board.get(1).get(1).color, new Color(200, 0, 0));
    t.checkExpect(test1.board.get(2).get(2).color, new Color(156, 125, 241));
    test1.userClicksCount = 3;
    test1.currentlyWaterfalling = true;

    FloodItWorld test2 = new FloodItWorld(6, 5, 8);
    test2.initBoard();
    test2.initColors();
    test2.initNeighbors();
    ArrayList<ArrayList<Cell>> boardGrab2 = test2.board;
    test2.propogateOrder = test2.propogateOrder();
    test2.currentlyWaterfalling = true;
    test2.floodColor = new Color(200, 0, 0);
    test2.maxClicksAllowed = 3;
    t.checkExpect(test2.board.get(0).get(0).color, new Color(55, 183, 115));
    t.checkExpect(test2.board.get(1).get(1).color, new Color(55, 183, 115));
    t.checkExpect(test2.board.get(2).get(2).color, new Color(3, 252, 160));

    test1.onTick();
    t.checkExpect(test2.board, boardGrab2);
    t.checkExpect(test2.propogateIndex, 0);
    t.checkExpect(test2.currentlyWaterfalling, true);
    t.checkExpect(test2.floodColor, new Color(200, 0, 0));
    t.checkExpect(test2.board.get(1).get(1).color, new Color(55, 183, 115));
    t.checkExpect(test2.board.get(2).get(2).color, new Color(3, 252, 160));
    test2.board.get(1).get(1).flooded();
    test2.board.get(2).get(2).flooded();
    test2.onTick();
    t.checkExpect(test2.propogateIndex, 1);
    t.checkExpect(test2.board.get(1).get(1).color, new Color(55, 183, 115));
    t.checkExpect(test2.board.get(2).get(2).color, new Color(3, 252, 160));
    test2.userClicksCount = 3;
    test2.currentlyWaterfalling = true;
  }

  // to test the method endScreen
  void testEndScreen(Tester t) {
    this.initTestExamples();

    WorldScene gameBoardTemp1 = new WorldScene(500, 500);
    gameBoardTemp1.placeImageXY(new TextImage("End of Game", 30, Color.black), 250, 70);
    world1.endScreen("End of Game");
    t.checkExpect(world1.gameBoard, gameBoardTemp1);

    this.initTestExamples();

    WorldScene gameBoardTemp2 = new WorldScene(500, 500);
    gameBoardTemp2.placeImageXY(new TextImage("The Game is Over", 30, Color.black), 250, 70);
    world2.endScreen("The Game is Over");
    t.checkExpect(world2.gameBoard, gameBoardTemp2);

    this.initTestExamples();

    WorldScene gameBoardTemp3 = new WorldScene(500, 500);
    gameBoardTemp3.placeImageXY(new TextImage("You Won", 30, Color.black), 250, 70);
    world3.endScreen("You Won");
    t.checkExpect(world3.gameBoard, gameBoardTemp3);

    this.initTestExamples();

    WorldScene gameBoardTemp4 = new WorldScene(500, 500);
    gameBoardTemp4.placeImageXY(new TextImage("You Lost", 30, Color.black), 250, 70);
    world4.endScreen("You Lost");
    t.checkExpect(world4.gameBoard, gameBoardTemp4);
  }

  // to test the method onKeyEvent
  void testOnKeyEvent(Tester t) {
    this.initTestExamples();

    world1.userClicksCount = 5;
    world2.userClicksCount = 0;
    world3.userClicksCount = 7;
    world4.userClicksCount = 8;

    world1.onKeyEvent("h");
    t.checkExpect(world1.userClicksCount, 5);

    world2.onKeyEvent("l");
    t.checkExpect(world2.userClicksCount, 0);

    world3.onKeyEvent("r");
    t.checkExpect(world3.userClicksCount, 0);

    world4.onKeyEvent("r");
    t.checkExpect(world4.userClicksCount, 0);
  }

  // to test the method onMouseClicked
  void testOnMouseClicked(Tester t) {
    this.initTestExamples();

    world1.floodColor = new Color(1, 127, 218);
    world1.onMouseClicked(new Posn(250, 250));
    t.checkExpect(world1.floodColor, new Color(252, 8, 219));

    world2.floodColor = new Color(1, 127, 218);
    world2.onMouseClicked(new Posn(215, 260));
    t.checkExpect(world2.floodColor, new Color(1, 127, 218));

    world3.floodColor = new Color(187, 232, 235);
    world3.onMouseClicked(new Posn(250, 250));
    t.checkExpect(world3.floodColor, new Color(173, 72, 20));

    world4.floodColor = new Color(173, 72, 20);
    world4.onMouseClicked(new Posn(250, 250));
    t.checkExpect(world4.floodColor, new Color(173, 72, 20));
  }

  // runs the game by creating a world and calling bigBang
  void testFloodIt(Tester t) {
    FloodItWorld starterWorld = new FloodItWorld(3, 7, 2);
    starterWorld.initColors();
    starterWorld.initBoard();
    starterWorld.initNeighbors();
    starterWorld.bigBang(500, 500, 25);
  }
}