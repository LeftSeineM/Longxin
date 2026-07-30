module core_top (
    input           aclk,
    input           aresetn,
    input    [ 7:0] intrpt,

    output   [ 3:0] arid,
    output   [31:0] araddr,
    output   [ 7:0] arlen,
    output   [ 2:0] arsize,
    output   [ 1:0] arburst,
    output   [ 1:0] arlock,
    output   [ 3:0] arcache,
    output   [ 2:0] arprot,
    output          arvalid,
    input           arready,

    input    [ 3:0] rid,
    input    [31:0] rdata,
    input    [ 1:0] rresp,
    input           rlast,
    input           rvalid,
    output          rready,

    output   [ 3:0] awid,
    output   [31:0] awaddr,
    output   [ 7:0] awlen,
    output   [ 2:0] awsize,
    output   [ 1:0] awburst,
    output   [ 1:0] awlock,
    output   [ 3:0] awcache,
    output   [ 2:0] awprot,
    output          awvalid,
    input           awready,

    output   [ 3:0] wid,
    output   [31:0] wdata,
    output   [ 3:0] wstrb,
    output          wlast,
    output          wvalid,
    input           wready,

    input    [ 3:0] bid,
    input    [ 1:0] bresp,
    input           bvalid,
    output          bready,

    input           break_point,
    input           infor_flag,
    input    [ 4:0] reg_num,
    output          ws_valid,
    output   [31:0] rf_rdata,

    output   [31:0] debug0_wb_pc,
    output   [ 3:0] debug0_wb_rf_wen,
    output   [ 4:0] debug0_wb_rf_wnum,
    output   [31:0] debug0_wb_rf_wdata,
    output   [31:0] debug0_wb_inst
);

    wire [0:0] wb_arlock;
    wire [0:0] wb_awlock;
    wire [2:0] retire_mask;
    wire [31:0] retire_pc_0;
    wire [31:0] retire_pc_1;
    wire [31:0] retire_pc_2;
    wire [31:0] retire_inst_0;
    wire [31:0] retire_inst_1;
    wire [31:0] retire_inst_2;
    wire [2:0] retire_wen;
    wire [4:0] retire_waddr_0;
    wire [4:0] retire_waddr_1;
    wire [4:0] retire_waddr_2;
    wire [31:0] retire_wdata_0;
    wire [31:0] retire_wdata_1;
    wire [31:0] retire_wdata_2;
    wire exception_valid;
    wire [5:0] exception_code;

    assign arlock = {1'b0, wb_arlock};
    assign awlock = {1'b0, wb_awlock};
    assign debug0_wb_pc = retire_mask[0] ? retire_pc_0 :
                          retire_mask[1] ? retire_pc_1 :
                          retire_mask[2] ? retire_pc_2 : 32'b0;
    assign debug0_wb_inst = retire_mask[0] ? retire_inst_0 :
                            retire_mask[1] ? retire_inst_1 :
                            retire_mask[2] ? retire_inst_2 : 32'b0;
    assign debug0_wb_rf_wen = retire_mask[0] ? {4{retire_wen[0]}} :
                              retire_mask[1] ? {4{retire_wen[1]}} :
                              retire_mask[2] ? {4{retire_wen[2]}} : 4'b0;
    assign debug0_wb_rf_wnum = retire_mask[0] ? retire_waddr_0 :
                               retire_mask[1] ? retire_waddr_1 :
                               retire_mask[2] ? retire_waddr_2 : 5'b0;
    assign debug0_wb_rf_wdata = retire_mask[0] ? retire_wdata_0 :
                                retire_mask[1] ? retire_wdata_1 :
                                retire_mask[2] ? retire_wdata_2 : 32'b0;

`ifndef SYNTHESIS
    wire [2:0] retire_mem_mask;
    wire [2:0] retire_branch_mask;
    wire [2:0] retire_pred_mask;
    wire [2:0] retire_mispred_mask;
    wire icache_miss;
    wire dcache_miss;

    wb_perf_counter perf_counter (
        .clk                 (aclk),
        .resetn              (aresetn),
        .retire_mask         (retire_mask),
        .retire_mem_mask     (retire_mem_mask),
        .retire_branch_mask  (retire_branch_mask),
        .retire_pred_mask    (retire_pred_mask),
        .retire_mispred_mask (retire_mispred_mask),
        .icache_miss         (icache_miss),
        .dcache_miss         (dcache_miss)
    );

    reg [31:0] last_retire_pc;
    reg [31:0] last_retire_inst;
    reg        low_pc_reported;
    reg        exception_reported;
    always @(posedge aclk) begin
        if (!aresetn) begin
            last_retire_pc   <= 32'b0;
            last_retire_inst <= 32'b0;
            low_pc_reported  <= 1'b0;
            exception_reported <= 1'b0;
        end else begin
            if (retire_mask[0]) begin
                last_retire_pc   <= retire_pc_0;
                last_retire_inst <= retire_inst_0;
            end
            if (retire_mask[1]) begin
                last_retire_pc   <= retire_pc_1;
                last_retire_inst <= retire_inst_1;
            end
            if (retire_mask[2]) begin
                last_retire_pc   <= retire_pc_2;
                last_retire_inst <= retire_inst_2;
            end
            if (exception_valid && !exception_reported) begin
                exception_reported <= 1'b1;
                $display("WB_EXCEPTION time=%0t code=%0d last_pc=%08x last_inst=%08x",
                         $time, exception_code, last_retire_pc, last_retire_inst);
            end
            if (!low_pc_reported &&
                ((retire_mask[0] && retire_pc_0 < 32'h00001000) ||
                 (retire_mask[1] && retire_pc_1 < 32'h00001000) ||
                 (retire_mask[2] && retire_pc_2 < 32'h00001000))) begin
                low_pc_reported <= 1'b1;
                $display("WB_LOW_PC time=%0t mask=%b pc=%08x/%08x/%08x inst=%08x/%08x/%08x",
                         $time, retire_mask,
                         retire_pc_0, retire_pc_1, retire_pc_2,
                         retire_inst_0, retire_inst_1, retire_inst_2);
            end
        end
    end
`endif

    wb_raw_top u_wb (
        .aclk               (aclk),
        .aresetn            (aresetn),
        .intrpt             (intrpt),

        .arid               (arid),
        .araddr             (araddr),
        .arlen              (arlen),
        .arsize             (arsize),
        .arburst            (arburst),
        .arlock             (wb_arlock),
        .arcache            (arcache),
        .arprot             (arprot),
        .arvalid            (arvalid),
        .arready            (arready),

        .rid                (rid),
        .rdata              (rdata),
        .rresp              (rresp),
        .rlast              (rlast),
        .rvalid             (rvalid),
        .rready             (rready),

        .awid               (awid),
        .awaddr             (awaddr),
        .awlen              (awlen),
        .awsize             (awsize),
        .awburst            (awburst),
        .awlock             (wb_awlock),
        .awcache            (awcache),
        .awprot             (awprot),
        .awvalid            (awvalid),
        .awready            (awready),

        .wid                (wid),
        .wdata              (wdata),
        .wstrb              (wstrb),
        .wlast              (wlast),
        .wvalid             (wvalid),
        .wready             (wready),

        .bid                (bid),
        .bresp              (bresp),
        .bvalid             (bvalid),
        .bready             (bready),

        .break_point        (break_point),
        .infor_flag         (infor_flag),
        .reg_num            (reg_num),
        .ws_valid           (ws_valid),
        .rf_rdata           (rf_rdata),

        .retireMask         (retire_mask),
        .retireAddr_0       (retire_pc_0),
        .retireAddr_1       (retire_pc_1),
        .retireAddr_2       (retire_pc_2),
        .retireInst_0       (retire_inst_0),
        .retireInst_1       (retire_inst_1),
        .retireInst_2       (retire_inst_2),
        .retireWen          (retire_wen),
        .retireWaddr_0      (retire_waddr_0),
        .retireWaddr_1      (retire_waddr_1),
        .retireWaddr_2      (retire_waddr_2),
        .retireWresult_0    (retire_wdata_0),
        .retireWresult_1    (retire_wdata_1),
        .retireWresult_2    (retire_wdata_2),
`ifndef SYNTHESIS
        .retireMemMask      (retire_mem_mask),
        .retireBranchMask   (retire_branch_mask),
        .retirePredMask     (retire_pred_mask),
        .retireMispredMask  (retire_mispred_mask),
        .icacheMiss         (icache_miss),
        .dcacheMiss         (dcache_miss),
`endif
        .exceptionValid     (exception_valid),
        .exceptionCode      (exception_code)
    );

endmodule

module wb_perf_counter (
    input        clk,
    input        resetn,
    input  [2:0] retire_mask,
    input  [2:0] retire_mem_mask,
    input  [2:0] retire_branch_mask,
    input  [2:0] retire_pred_mask,
    input  [2:0] retire_mispred_mask,
    input        icache_miss,
    input        dcache_miss
);
    reg [63:0] cycle_counter;
    reg [63:0] commit_inst_counter;
    reg [63:0] icache_miss_counter;
    reg [63:0] dcache_miss_counter;
    reg [63:0] mem_inst_counter;
    reg [63:0] br_inst_counter;
    reg [63:0] br_pre_counter;
    reg [63:0] br_pre_error_counter;
    reg [63:0] dual_commit_cycle;
    reg [63:0] triple_commit_cycle;

    wire [1:0] retire_count =
        {1'b0, retire_mask[0]} +
        {1'b0, retire_mask[1]} +
        {1'b0, retire_mask[2]};
    wire [1:0] mem_count =
        {1'b0, retire_mem_mask[0]} +
        {1'b0, retire_mem_mask[1]} +
        {1'b0, retire_mem_mask[2]};
    wire [1:0] branch_count =
        {1'b0, retire_branch_mask[0]} +
        {1'b0, retire_branch_mask[1]} +
        {1'b0, retire_branch_mask[2]};
    wire [1:0] pred_count =
        {1'b0, retire_pred_mask[0]} +
        {1'b0, retire_pred_mask[1]} +
        {1'b0, retire_pred_mask[2]};
    wire [1:0] mispred_count =
        {1'b0, retire_mispred_mask[0]} +
        {1'b0, retire_mispred_mask[1]} +
        {1'b0, retire_mispred_mask[2]};

    always @(posedge clk) begin
        if (!resetn) begin
            cycle_counter        <= 64'b0;
            commit_inst_counter  <= 64'b0;
            icache_miss_counter  <= 64'b0;
            dcache_miss_counter  <= 64'b0;
            mem_inst_counter     <= 64'b0;
            br_inst_counter      <= 64'b0;
            br_pre_counter       <= 64'b0;
            br_pre_error_counter <= 64'b0;
            dual_commit_cycle    <= 64'b0;
            triple_commit_cycle  <= 64'b0;
        end else begin
            cycle_counter       <= cycle_counter + 64'd1;
            commit_inst_counter <= commit_inst_counter + retire_count;
            icache_miss_counter <= icache_miss_counter + icache_miss;
            dcache_miss_counter <= dcache_miss_counter + dcache_miss;
            mem_inst_counter <= mem_inst_counter + mem_count;
            br_inst_counter <= br_inst_counter + branch_count;
            br_pre_counter <= br_pre_counter + pred_count;
            br_pre_error_counter <= br_pre_error_counter + mispred_count;
            if (retire_count >= 2)
                dual_commit_cycle <= dual_commit_cycle + 64'd1;
            if (retire_count == 3)
                triple_commit_cycle <= triple_commit_cycle + 64'd1;
        end
    end
endmodule
