#!/bin/bash

# USAGE
#
# detangle_all_accounts <path_to_monthly_accounting_folder> <path_to_previous_month_accounting_folder>

# Parse input arguments
month_dir=$1
prev_month_dir=$2

echo "Detangling all accounts in ${month_dir} using previous statements from ${prev_month_dir}"

echo "If a statement can't be found then the relevant detangler will fail with a scallop error, but this script will continue anyway."

bank_dir="${month_dir}/bank_statements"
etsy_dir="${month_dir}/etsy_sales"
etsy_dir_prev_month="${prev_month_dir}/etsy_sales"
square_dir="${month_dir}/square_sales"

bank_statement="$(find ${bank_dir} -type f -iname "TransactionHistory*.csv")"
etsy_statement="$(find ${etsy_dir} -type f -iname "etsy_statement*.csv")"
etsy_sold_orders="$(find ${etsy_dir} -type f -iname "EtsySoldOrders*.csv")"
etsy_sold_orders_prev_month="$(find ${etsy_dir_prev_month} -type f -iname "EtsySoldOrders*.csv")"
square_deposit_details="$(find ${square_dir} -type f -iname "transfer-details*.csv")"
square_transactions="$(find ${square_dir} -type f -iname "transactions*.csv")"

echo "Found bank statement in ${bank_statement}"
echo "Found Etsy statement in ${etsy_statement}"
echo "Found Etsy sold orders in ${etsy_sold_orders}"
echo "Found Esty sold orders previous month in ${etsy_sold_orders_prev_month}"
echo "Found Square deposit details in ${square_deposit_details}"
echo "Found Square transactions in ${square_transactions}"

echo "Detangling bank statement...."
java -jar nab_statement_converter/target/scala-2.13/nab_csv_converter-assembly-0.1.jar -n ${bank_statement} -o ${bank_dir}
echo "Done."

echo "Detangling Etsy statement...."
java -jar etsy_detangler/target/scala-2.13/etsy_detangler-assembly-1.0.jar -d ${etsy_sold_orders} -e ${etsy_sold_orders_prev_month} -s ${etsy_statement} -o ${etsy_dir}
echo "Done."

echo "Detangling Square statement...."
java -jar square_detangler/target/scala-2.13/square_detangler-assembly-0.1.jar -d ${square_deposit_details} -t ${square_transactions} -o ${square_dir}
echo "Done."
