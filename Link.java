package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;
  Short weight;
  boolean isAlive;

  public Link(RouterDescription r1, RouterDescription r2, Short weight) {
    router1 = r1;
    router2 = r2;
    this.weight = weight;
  }
}
