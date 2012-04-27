Diffa.Helpers.Viz = {
  /**
   * Transforms the bucket value from the API into a value we can use for
   * sizing the blob in the heatmap.
   *
   *   API   -> Size
   *   0     -> 0
   *   1     -> m     (1)
   *   ...
   *   100   -> M     (2)
   *
   * M is the maximum size. m is the minimum display size.
   * Between (1) and (2), growth should be logarithmic based
   * on the endpoints. Inputs over 100 follow the log function,
   * to be limited in the caller.
   *
   * We'd like a function f on [0, 100] where
   *
   *   f(1)   = m,
   *   f(100) = M
   *
   * and where f(x) is in some way logarithmic. So let
   *
   *   f(x) = a + b*log(x).
   *
   * (Note this is equivalent to f(x) = a + b*log(c*x))
   *
   * and solve for a, b:
   *
   *   f(1)   = a = m
   *   f(100) = m + b*log(100) = M  =>  b = (M-m)/log(100)
   */
  transformBucketSize: function(size, maximum) {
    var minimumIn = 1; // anything non-zero below this gets raised to valueFloor
    var valueFloor = 2;
    var maximumCutOff = 100; // anything over this is capped to the maximum

    if (size == 0)             { return 0; }
    if (size <= minimumIn)     { return valueFloor; }

    var a = valueFloor;
    var b = (maximum - valueFloor)/Math.log(maximumCutOff);

    return a + b*Math.log(size);
  },

  /**
   * Limits a value to a given maximum value, somewhat like Math.min().
   * Returns an object with two properties: "value", the limited value and "limited",
   * which flags whether the original value was greater than the maximum value.
   */
  limit: function(value, maximum) {
    if (value <= maximum) {
      return {"value":value, "limited":false};
    }
    return {"value":maximum, "limited":true};
  }
};