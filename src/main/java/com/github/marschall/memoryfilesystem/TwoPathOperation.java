package com.github.marschall.memoryfilesystem;

enum TwoPathOperation {

  COPY {
    @Override
    boolean isMove() {
      return false;
    }
  },


  MOVE {
    @Override
    boolean isMove() {
      return true;
    }
  };

  abstract boolean isMove();

}