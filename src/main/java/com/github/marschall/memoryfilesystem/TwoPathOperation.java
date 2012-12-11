package com.github.marschall.memoryfilesystem;

enum TwoPathOperation {

  COPY {
    @Override
    boolean isCopy() {
      return true;
    }

    @Override
    boolean isMove() {
      return false;
    }
  },


  MOVE {
    @Override
    boolean isCopy() {
      return false;
    }

    @Override
    boolean isMove() {
      return true;
    }
  };

  abstract boolean isCopy();

  abstract boolean isMove();

}